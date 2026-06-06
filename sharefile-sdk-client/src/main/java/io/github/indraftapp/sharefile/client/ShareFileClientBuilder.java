package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.auth.InMemoryTokenStore;
import io.github.indraftapp.sharefile.client.auth.StaticCredentialProvider;
import io.github.indraftapp.sharefile.client.auth.TokenManager;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.http.JdkHttpTransport;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.github.indraftapp.sharefile.client.spi.TokenStore;
import io.github.indraftapp.sharefile.core.exception.CredentialResolutionException;
import io.github.indraftapp.sharefile.core.jackson.ShareFileObjectMapper;
import io.github.indraftapp.sharefile.core.model.OAuthToken;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Fluent builder for constructing {@link ShareFileClient}. */
public final class ShareFileClientBuilder {

  private String subdomain;
  private String apiControlPlane;
  private Duration connectTimeout;
  private Duration readTimeout;
  private Duration uploadTimeout;
  private Duration downloadTimeout;
  private Duration tokenRefreshBuffer;
  private String baseUrl;

  private CredentialProvider credentialProvider;
  private String clientId;
  private String clientSecret;
  private String authorizationCode;
  private String authorizationCodeRedirectUri;
  private String username;
  private String password;
  private String accessToken;
  private String refreshToken;

  private HttpTransport httpTransport;
  private TokenStore tokenStore;
  private MetricsProvider metricsProvider;
  private ObjectMapper objectMapper;
  private ExecutorService executor;
  private RetryConfig retryConfig;

  public ShareFileClientBuilder subdomain(String subdomain) {
    this.subdomain = Objects.requireNonNull(subdomain, "subdomain must not be null");
    return this;
  }

  public ShareFileClientBuilder credentialProvider(CredentialProvider provider) {
    this.credentialProvider = Objects.requireNonNull(provider, "provider must not be null");
    return this;
  }

  public ShareFileClientBuilder clientCredentials(String clientId, String clientSecret) {
    this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
    this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
    return this;
  }

  public ShareFileClientBuilder passwordGrant(String username, String password) {
    this.username = Objects.requireNonNull(username, "username must not be null");
    this.password = Objects.requireNonNull(password, "password must not be null");
    return this;
  }

  public ShareFileClientBuilder authorizationCode(String code) {
    this.authorizationCode = Objects.requireNonNull(code, "code must not be null");
    this.authorizationCodeRedirectUri = null;
    return this;
  }

  public ShareFileClientBuilder authorizationCodeRedirectUri(String redirectUri) {
    this.authorizationCodeRedirectUri =
        Objects.requireNonNull(redirectUri, "redirectUri must not be null");
    this.authorizationCode = null;
    return this;
  }

  public ShareFileClientBuilder accessToken(String accessToken, String refreshToken) {
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
    this.refreshToken = refreshToken;
    return this;
  }

  public ShareFileClientBuilder apiControlPlane(String apiControlPlane) {
    this.apiControlPlane =
        Objects.requireNonNull(apiControlPlane, "apiControlPlane must not be null");
    return this;
  }

  public ShareFileClientBuilder connectTimeout(Duration timeout) {
    this.connectTimeout = Objects.requireNonNull(timeout, "timeout must not be null");
    return this;
  }

  public ShareFileClientBuilder readTimeout(Duration timeout) {
    this.readTimeout = Objects.requireNonNull(timeout, "timeout must not be null");
    return this;
  }

  public ShareFileClientBuilder uploadTimeout(Duration timeout) {
    this.uploadTimeout = Objects.requireNonNull(timeout, "timeout must not be null");
    return this;
  }

  public ShareFileClientBuilder downloadTimeout(Duration timeout) {
    this.downloadTimeout = Objects.requireNonNull(timeout, "timeout must not be null");
    return this;
  }

  public ShareFileClientBuilder httpTransport(HttpTransport transport) {
    this.httpTransport = Objects.requireNonNull(transport, "transport must not be null");
    return this;
  }

  public ShareFileClientBuilder tokenStore(TokenStore tokenStore) {
    this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore must not be null");
    return this;
  }

  public ShareFileClientBuilder metricsProvider(MetricsProvider metricsProvider) {
    this.metricsProvider =
        Objects.requireNonNull(metricsProvider, "metricsProvider must not be null");
    return this;
  }

  public ShareFileClientBuilder objectMapper(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    return this;
  }

  public ShareFileClientBuilder executor(ExecutorService executor) {
    this.executor = Objects.requireNonNull(executor, "executor must not be null");
    return this;
  }

  public ShareFileClientBuilder retryConfig(RetryConfig retryConfig) {
    this.retryConfig = Objects.requireNonNull(retryConfig, "retryConfig must not be null");
    return this;
  }

  public ShareFileClientBuilder tokenRefreshBuffer(Duration tokenRefreshBuffer) {
    this.tokenRefreshBuffer =
        Objects.requireNonNull(tokenRefreshBuffer, "tokenRefreshBuffer must not be null");
    return this;
  }

  public ShareFileClientBuilder baseUrl(String baseUrl) {
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    return this;
  }

  public ShareFileClient build() {
    validate();

    ShareFileConfig config = buildConfig();
    ObjectMapper resolvedObjectMapper =
        objectMapper == null ? ShareFileObjectMapper.create() : objectMapper;
    MetricsProvider resolvedMetrics =
        metricsProvider == null ? MetricsProvider.noop() : metricsProvider;
    RetryConfig resolvedRetryConfig = retryConfig == null ? RetryConfig.defaults() : retryConfig;
    TokenStore resolvedTokenStore = tokenStore == null ? new InMemoryTokenStore() : tokenStore;

    boolean ownsTransport = httpTransport == null;
    HttpTransport resolvedTransport = ownsTransport ? new JdkHttpTransport(config) : httpTransport;

    boolean ownsExecutor = executor == null;
    ExecutorService resolvedExecutor = ownsExecutor ? createDefaultExecutor() : executor;
    TokenManager tokenManager = null;
    try {
      CredentialProvider resolvedProvider = resolveCredentialProvider();
      OAuthToken initialToken = buildInitialToken();

      tokenManager =
          createTokenManager(
              config,
              resolvedProvider,
              resolvedTransport,
              resolvedTokenStore,
              resolvedObjectMapper,
              resolvedMetrics,
              initialToken);

      ShareFileHttpClient httpClient =
          new ShareFileHttpClient(
              resolvedTransport,
              tokenManager,
              resolvedObjectMapper,
              resolvedRetryConfig,
              resolvedMetrics,
              baseUrl == null ? config.getBaseUrl() : baseUrl,
              config.getReadTimeout());

      ItemsClient itemsClient = new ItemsClient(httpClient);
      UsersClient usersClient = new UsersClient(httpClient);
      SharesClient sharesClient = new SharesClient(httpClient);
      AccessControlsClient accessControlsClient = new AccessControlsClient(httpClient);
      AsyncOperationsClient asyncOperationsClient = new AsyncOperationsClient(httpClient);
      TransferClient transferClient =
          new TransferClient(
              httpClient,
              resolvedTransport,
              resolvedObjectMapper,
              config,
              resolvedRetryConfig,
              resolvedMetrics,
              resolvedExecutor);
      GroupsClient groupsClient = new GroupsClient(httpClient);
      AccountsClient accountsClient = new AccountsClient(httpClient);
      ZonesClient zonesClient = new ZonesClient();
      WebhookSubscriptionsClient webhookSubscriptionsClient =
          new WebhookSubscriptionsClient(httpClient);
      SessionsClient sessionsClient = new SessionsClient(httpClient);

      return new ShareFileClient(
          itemsClient,
          usersClient,
          sharesClient,
          accessControlsClient,
          asyncOperationsClient,
          transferClient,
          groupsClient,
          accountsClient,
          zonesClient,
          webhookSubscriptionsClient,
          sessionsClient,
          tokenManager,
          resolvedExecutor,
          ownsExecutor,
          resolvedTransport,
          ownsTransport);
    } catch (RuntimeException e) {
      if (tokenManager != null) {
        tokenManager.close();
      }
      if (ownsExecutor) {
        resolvedExecutor.shutdownNow();
      }
      throw e;
    }
  }

  private void validate() {
    if (subdomain == null || subdomain.isBlank()) {
      throw new IllegalStateException("subdomain must be configured");
    }
    if ((authorizationCode != null || authorizationCodeRedirectUri != null) && username != null) {
      throw new IllegalStateException(
          "authorizationCode() and passwordGrant() are mutually exclusive");
    }
    if (authorizationCode != null && authorizationCodeRedirectUri != null) {
      throw new IllegalStateException(
          "authorizationCode() and authorizationCodeRedirectUri() are mutually exclusive");
    }
    if ((authorizationCode != null || authorizationCodeRedirectUri != null || username != null)
        && (clientId == null || clientSecret == null)) {
      throw new IllegalStateException(
          "authorizationCode(), authorizationCodeRedirectUri(), and passwordGrant() require "
              + "clientCredentials()");
    }
    if (accessToken != null && (clientId == null || clientSecret == null)) {
      throw new IllegalStateException("accessToken() requires clientCredentials()");
    }
    if (credentialProvider != null
        && (authorizationCode != null
            || authorizationCodeRedirectUri != null
            || username != null)) {
      throw new IllegalStateException(
          "credentialProvider() cannot be combined with authorizationCode(), "
              + "authorizationCodeRedirectUri(), or passwordGrant()");
    }

    boolean hasProvider = credentialProvider != null;
    boolean hasGrant =
        authorizationCode != null || authorizationCodeRedirectUri != null || username != null;
    boolean hasSeedToken = accessToken != null;
    if (!hasProvider && !hasGrant && !hasSeedToken) {
      throw new IllegalStateException("One authentication approach must be configured");
    }
    if (!hasProvider && !hasSeedToken && clientId != null && clientSecret != null && !hasGrant) {
      throw new IllegalStateException("clientCredentials() alone is not a complete auth flow");
    }
  }

  private ShareFileConfig buildConfig() {
    ShareFileConfig.Builder configBuilder = ShareFileConfig.builder().subdomain(subdomain);
    if (apiControlPlane != null) {
      configBuilder.apiControlPlane(apiControlPlane);
    }
    if (connectTimeout != null) {
      configBuilder.connectTimeout(connectTimeout);
    }
    if (readTimeout != null) {
      configBuilder.readTimeout(readTimeout);
    }
    if (uploadTimeout != null) {
      configBuilder.uploadTimeout(uploadTimeout);
    }
    if (downloadTimeout != null) {
      configBuilder.downloadTimeout(downloadTimeout);
    }
    if (tokenRefreshBuffer != null) {
      configBuilder.tokenRefreshBuffer(tokenRefreshBuffer);
    }
    return configBuilder.build();
  }

  private CredentialProvider resolveCredentialProvider() {
    if (credentialProvider != null) {
      return credentialProvider;
    }

    if (authorizationCode != null || authorizationCodeRedirectUri != null) {
      Credentials.Builder builder = Credentials.builder().clientCredentials(clientId, clientSecret);
      Credentials credentials =
          authorizationCodeRedirectUri != null
              ? builder.authorizationCodeRedirectUri(authorizationCodeRedirectUri).build()
              : builder.authorizationCode(authorizationCode).build();
      return new StaticCredentialProvider(credentials);
    }
    if (username != null) {
      return new StaticCredentialProvider(
          Credentials.builder()
              .clientCredentials(clientId, clientSecret)
              .passwordGrant(username, password)
              .build());
    }
    return () -> {
      throw new CredentialResolutionException(
          "No credential provider or grant credentials available for full re-authentication");
    };
  }

  private OAuthToken buildInitialToken() {
    if (accessToken == null) {
      return null;
    }

    OAuthToken token = new OAuthToken();
    token.setAccessToken(accessToken);
    token.setRefreshToken(refreshToken);
    return token;
  }

  private TokenManager createTokenManager(
      ShareFileConfig config,
      CredentialProvider resolvedProvider,
      HttpTransport resolvedTransport,
      TokenStore resolvedTokenStore,
      ObjectMapper resolvedObjectMapper,
      MetricsProvider resolvedMetrics,
      OAuthToken initialToken) {
    if (initialToken != null) {
      if (clientId != null && clientSecret != null) {
        return new TokenManager(
            config,
            clientId,
            clientSecret,
            resolvedProvider,
            resolvedTransport,
            resolvedTokenStore,
            resolvedObjectMapper,
            resolvedMetrics,
            initialToken);
      }
      return new TokenManager(
          config,
          resolvedProvider,
          resolvedTransport,
          resolvedTokenStore,
          resolvedObjectMapper,
          resolvedMetrics,
          initialToken);
    }

    if (clientId != null && clientSecret != null) {
      return new TokenManager(
          config,
          clientId,
          clientSecret,
          resolvedProvider,
          resolvedTransport,
          resolvedTokenStore,
          resolvedObjectMapper,
          resolvedMetrics);
    }
    return new TokenManager(
        config,
        resolvedProvider,
        resolvedTransport,
        resolvedTokenStore,
        resolvedObjectMapper,
        resolvedMetrics);
  }

  private static ExecutorService createDefaultExecutor() {
    int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
    return Executors.newFixedThreadPool(
        threads,
        runnable -> {
          Thread thread = new Thread(runnable, "sharefile-client-async");
          thread.setDaemon(true);
          return thread;
        });
  }
}
