package io.github.indraftapp.sharefile.client.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.client.spi.TokenStore;
import io.github.indraftapp.sharefile.core.exception.CredentialResolutionException;
import io.github.indraftapp.sharefile.core.exception.ShareFileAuthenticationException;
import io.github.indraftapp.sharefile.core.model.OAuthToken;
import io.github.indraftapp.sharefile.core.model.enums.GrantType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.StringJoiner;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages OAuth2 access tokens for the ShareFile API.
 *
 * <p>The token lifecycle follows a 4-step flow:
 *
 * <ol>
 *   <li>Check {@link TokenStore} for a cached valid token (fast path).
 *   <li>If expired and a refresh token is available, perform a {@code refresh_token} grant using
 *       only {@code client_id}, {@code client_secret}, and {@code refresh_token} — the {@link
 *       CredentialProvider} is NOT called.
 *   <li>If no token exists, no refresh token, or refresh failed — call {@link
 *       CredentialProvider#resolve()} and perform the configured grant (password or
 *       authorization_code).
 *   <li>Proactive refresh at 80% of {@code expires_in} (background). On failure, falls back to step
 *       3.
 * </ol>
 *
 * <p>This class is thread-safe. Concurrent callers share the same valid token via a {@link
 * ReentrantReadWriteLock}.
 */
public final class TokenManager implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(TokenManager.class);
  private static final int MAX_REFRESH_RETRIES = 3;
  private static final Duration[] RETRY_BACKOFFS = {
    Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(4)
  };
  private static final double PROACTIVE_REFRESH_RATIO = 0.80;

  private final ShareFileConfig config;
  private final String clientId;
  private final String clientSecret;
  private final CredentialProvider credentialProvider;
  private final HttpTransport transport;
  private final TokenStore tokenStore;
  private final ObjectMapper objectMapper;
  private volatile OAuthToken currentToken;
  private final ReentrantReadWriteLock tokenLock = new ReentrantReadWriteLock();
  private final ScheduledExecutorService scheduler;
  private volatile ScheduledFuture<?> scheduledRefresh;

  /**
   * Creates a new TokenManager.
   *
   * @param config SDK configuration
   * @param clientId OAuth2 client ID (extracted at build time)
   * @param clientSecret OAuth2 client secret (extracted at build time)
   * @param credentialProvider fallback credential provider for full re-auth
   * @param transport raw HTTP transport (no auth headers)
   * @param tokenStore token persistence backend
   * @param objectMapper Jackson ObjectMapper for parsing token responses
   */
  public TokenManager(
      ShareFileConfig config,
      String clientId,
      String clientSecret,
      CredentialProvider credentialProvider,
      HttpTransport transport,
      TokenStore tokenStore,
      ObjectMapper objectMapper) {
    this.config = Objects.requireNonNull(config, "config must not be null");
    this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
    this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
    this.credentialProvider =
        Objects.requireNonNull(credentialProvider, "credentialProvider must not be null");
    this.transport = Objects.requireNonNull(transport, "transport must not be null");
    this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");

    ScheduledThreadPoolExecutor exec =
        new ScheduledThreadPoolExecutor(
            1,
            r -> {
              Thread t = new Thread(r, "sharefile-token-refresh");
              t.setDaemon(true);
              return t;
            });
    exec.setRemoveOnCancelPolicy(true);
    this.scheduler = exec;

    // Load cached token from store on startup. Even expired tokens are loaded if they have a
    // refresh token, so step 2 (refresh_token grant) can be attempted before falling back to
    // full re-authentication.
    tokenStore
        .load()
        .ifPresent(
            token -> {
              this.currentToken = token;
              if (hasUsableAccessToken(token)) {
                scheduleProactiveRefresh(token);
              }
            });
  }

  /**
   * Creates a TokenManager with a pre-existing token (e.g., from {@code accessToken()} builder
   * method).
   *
   * @param config SDK configuration
   * @param clientId OAuth2 client ID
   * @param clientSecret OAuth2 client secret
   * @param credentialProvider fallback credential provider
   * @param transport raw HTTP transport
   * @param tokenStore token persistence backend
   * @param objectMapper Jackson ObjectMapper
   * @param initialToken pre-existing OAuth token
   */
  public TokenManager(
      ShareFileConfig config,
      String clientId,
      String clientSecret,
      CredentialProvider credentialProvider,
      HttpTransport transport,
      TokenStore tokenStore,
      ObjectMapper objectMapper,
      OAuthToken initialToken) {
    this(config, clientId, clientSecret, credentialProvider, transport, tokenStore, objectMapper);
    if (initialToken != null) {
      if (initialToken.getExpiresAt() == null && initialToken.getExpiresIn() != null) {
        initialToken.computeExpiresAt();
      }
      this.currentToken = initialToken;
      tokenStore.save(initialToken);
      scheduleProactiveRefresh(initialToken);
    }
  }

  /**
   * Returns a valid access token, performing authentication or refresh as needed.
   *
   * <p>This method is thread-safe. Multiple concurrent callers will share the same token and will
   * not trigger duplicate authentication requests.
   *
   * @return a valid access token string
   * @throws ShareFileAuthenticationException if authentication fails after all retries
   */
  public String getAccessToken() {
    // Step 1: Fast path — check cached token with read lock
    tokenLock.readLock().lock();
    try {
      if (hasUsableAccessToken(currentToken)) {
        return currentToken.getAccessToken();
      }
    } finally {
      tokenLock.readLock().unlock();
    }

    // Need to refresh or authenticate — upgrade to write lock
    tokenLock.writeLock().lock();
    try {
      // Double-check after acquiring write lock (another thread may have refreshed)
      if (hasUsableAccessToken(currentToken)) {
        return currentToken.getAccessToken();
      }

      return refreshOrAuthenticate(false);
    } finally {
      tokenLock.writeLock().unlock();
    }
  }

  /**
   * Forces a token refresh or full re-authentication even when a cached access token still appears
   * locally valid.
   *
   * <p>This is used after a 401 response, where the server has rejected the current bearer token
   * even though its local expiration timestamp may not have elapsed yet.
   *
   * @return a newly acquired access token
   * @throws ShareFileAuthenticationException if refresh and full re-authentication both fail
   */
  public String refreshAccessToken() {
    tokenLock.writeLock().lock();
    try {
      return refreshOrAuthenticate(true);
    } finally {
      tokenLock.writeLock().unlock();
    }
  }

  /**
   * Returns the current token, or {@code null} if no token has been acquired yet.
   *
   * @return the current OAuth token, possibly null
   */
  public OAuthToken getCurrentToken() {
    tokenLock.readLock().lock();
    try {
      return currentToken;
    } finally {
      tokenLock.readLock().unlock();
    }
  }

  @Override
  public void close() {
    if (scheduledRefresh != null) {
      scheduledRefresh.cancel(false);
    }
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  // ── Internal token operations ─────────────────────────────────────────

  private OAuthToken performRefreshWithRetries() {
    ShareFileAuthenticationException lastException = null;
    for (int attempt = 0; attempt < MAX_REFRESH_RETRIES; attempt++) {
      try {
        return performRefreshTokenGrant();
      } catch (ShareFileAuthenticationException e) {
        lastException = e;
        if (attempt < MAX_REFRESH_RETRIES - 1) {
          sleepForRetry(RETRY_BACKOFFS[attempt]);
        }
      }
    }
    throw new ShareFileAuthenticationException(
        "Token refresh failed after " + MAX_REFRESH_RETRIES + " attempts", lastException);
  }

  /**
   * Performs a refresh_token grant using only client_id, client_secret, and refresh_token. Does NOT
   * call CredentialProvider.resolve().
   */
  private OAuthToken performRefreshTokenGrant() {
    StringJoiner formBody = new StringJoiner("&");
    formBody.add("grant_type=" + encode(GrantType.REFRESH_TOKEN.getValue()));
    formBody.add("client_id=" + encode(clientId));
    formBody.add("client_secret=" + encode(clientSecret));
    formBody.add("refresh_token=" + encode(currentToken.getRefreshToken()));

    return executeTokenRequest(formBody.toString());
  }

  /** Performs full authentication by calling CredentialProvider.resolve(). */
  private OAuthToken performFullAuthentication() {
    Credentials credentials;
    try {
      credentials = credentialProvider.resolve();
    } catch (CredentialResolutionException e) {
      throw new ShareFileAuthenticationException("Failed to resolve credentials", e);
    }

    StringJoiner formBody = new StringJoiner("&");
    formBody.add("grant_type=" + encode(credentials.grantType().getValue()));
    formBody.add("client_id=" + encode(credentials.clientId()));
    formBody.add("client_secret=" + encode(credentials.clientSecret()));

    switch (credentials.grantType()) {
      case PASSWORD -> {
        formBody.add("username=" + encode(credentials.username()));
        formBody.add("password=" + encode(credentials.password()));
      }
      case AUTHORIZATION_CODE -> formBody.add("code=" + encode(credentials.authorizationCode()));
      default ->
          throw new ShareFileAuthenticationException(
              "Unsupported grant type: " + credentials.grantType());
    }

    return executeTokenRequest(formBody.toString());
  }

  private OAuthToken executeTokenRequest(String formBody) {
    URI tokenEndpoint = URI.create(config.getTokenEndpointUrl());
    byte[] bodyBytes = formBody.getBytes(StandardCharsets.UTF_8);

    var request = buildFormEncodedRequest(tokenEndpoint, bodyBytes, config.getReadTimeout());

    try (HttpTransport.HttpResponse response = transport.execute(request)) {
      byte[] responseBody = response.bodyBytes(1024 * 1024); // 1 MB max for token responses

      if (response.statusCode() >= 400) {
        String errorBody = new String(responseBody, StandardCharsets.UTF_8);
        throw new ShareFileAuthenticationException(
            "Token endpoint returned HTTP %d: %s".formatted(response.statusCode(), errorBody));
      }

      OAuthToken token = objectMapper.readValue(responseBody, OAuthToken.class);
      token.computeExpiresAt();
      return token;
    } catch (ShareFileAuthenticationException e) {
      throw e;
    } catch (IOException e) {
      throw new ShareFileAuthenticationException("Failed to parse token response", e);
    } catch (Exception e) {
      throw new ShareFileAuthenticationException("Token request failed", e);
    }
  }

  private void applyToken(OAuthToken token) {
    this.currentToken = token;
    tokenStore.save(token);
    scheduleProactiveRefresh(token);
  }

  private void scheduleProactiveRefresh(OAuthToken token) {
    // Cancel any existing scheduled refresh
    if (scheduledRefresh != null) {
      scheduledRefresh.cancel(false);
    }

    if (token.getExpiresIn() == null || token.getExpiresIn() <= 0) {
      return;
    }

    long refreshDelaySeconds = computeRefreshDelaySeconds(token);

    scheduledRefresh =
        scheduler.schedule(this::proactiveRefresh, refreshDelaySeconds, TimeUnit.SECONDS);
  }

  /** Step 4: Proactive background refresh. */
  private void proactiveRefresh() {
    tokenLock.writeLock().lock();
    try {
      if (currentToken == null || currentToken.getRefreshToken() == null) {
        return;
      }

      try {
        OAuthToken refreshed = performRefreshWithRetries();
        applyToken(refreshed);
        LOG.debug("Proactive token refresh succeeded");
      } catch (ShareFileAuthenticationException e) {
        LOG.warn("Proactive refresh failed, attempting full re-auth", e);
        try {
          OAuthToken token = performFullAuthentication();
          applyToken(token);
          LOG.debug("Full re-authentication succeeded after proactive refresh failure");
        } catch (ShareFileAuthenticationException reAuthEx) {
          LOG.error("Full re-authentication also failed", reAuthEx);
          // Token will be refreshed on next getAccessToken() call
        }
      }
    } finally {
      tokenLock.writeLock().unlock();
    }
  }

  /**
   * Builds a form-encoded HTTP request for the token endpoint. This avoids depending on the
   * package-private {@code HttpRequests} class in the {@code http} package.
   */
  private static HttpTransport.HttpRequest buildFormEncodedRequest(
      URI uri, byte[] body, Duration timeout) {
    var headers = new LinkedHashMap<String, String>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept", "application/json");
    Map<String, String> unmodifiableHeaders = Collections.unmodifiableMap(headers);

    return new HttpTransport.HttpRequest() {
      @Override
      public String method() {
        return "POST";
      }

      @Override
      public URI uri() {
        return uri;
      }

      @Override
      public Map<String, String> headers() {
        return unmodifiableHeaders;
      }

      @Override
      public Optional<InputStream> bodyStream() {
        return Optional.of(new ByteArrayInputStream(body));
      }

      @Override
      public OptionalLong contentLength() {
        return OptionalLong.of(body.length);
      }

      @Override
      public Duration timeout() {
        return timeout;
      }
    };
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String refreshOrAuthenticate(boolean forcedRefresh) {
    if (currentToken != null && currentToken.getRefreshToken() != null) {
      try {
        OAuthToken refreshed = performRefreshWithRetries();
        applyToken(refreshed);
        return refreshed.getAccessToken();
      } catch (ShareFileAuthenticationException e) {
        if (forcedRefresh) {
          LOG.warn("Forced token refresh failed, falling back to full re-auth", e);
        } else {
          LOG.warn("Token refresh failed, falling back to full re-auth", e);
        }
      }
    }

    OAuthToken token = performFullAuthentication();
    applyToken(token);
    return token.getAccessToken();
  }

  private long computeRefreshDelaySeconds(OAuthToken token) {
    long expiresInSeconds = token.getExpiresIn();
    long ratioDelaySeconds = (long) (expiresInSeconds * PROACTIVE_REFRESH_RATIO);
    long bufferDelaySeconds =
        expiresInSeconds - Math.max(0L, config.getTokenRefreshBuffer().getSeconds());
    return Math.max(0L, Math.min(ratioDelaySeconds, bufferDelaySeconds));
  }

  private static boolean hasUsableAccessToken(OAuthToken token) {
    return token != null && token.getExpiresAt() != null && !token.isExpired();
  }

  private static void sleepForRetry(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ShareFileAuthenticationException("Interrupted during token refresh retry", e);
    }
  }
}
