package io.github.indraftapp.sharefile.client.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.core.exception.CredentialResolutionException;
import io.github.indraftapp.sharefile.core.exception.ShareFileAuthenticationException;
import io.github.indraftapp.sharefile.core.model.OAuthToken;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link TokenManager}. */
class TokenManagerTest {

  private static final String CLIENT_ID = "test-client-id";
  private static final String CLIENT_SECRET = "test-client-secret";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private ShareFileConfig config;
  private InMemoryTokenStore tokenStore;
  private TokenManager tokenManager;
  private MockTransport mockTransport;
  private CountingCredentialProvider credentialProvider;

  @BeforeEach
  void setUp() {
    config = ShareFileConfig.builder().subdomain("testcompany").build();
    tokenStore = new InMemoryTokenStore();
    mockTransport = new MockTransport();
    credentialProvider = new CountingCredentialProvider();
  }

  @AfterEach
  void tearDown() {
    if (tokenManager != null) {
      tokenManager.close();
    }
  }

  // ── Password grant tests ──────────────────────────────────────────────

  @Test
  void passwordGrantSendsCorrectParams() {
    mockTransport.enqueueTokenResponse("access-1", "refresh-1", 3600);
    tokenManager = createTokenManager();

    String token = tokenManager.getAccessToken();

    assertEquals("access-1", token);
    assertEquals(1, mockTransport.requests.size());

    String body = mockTransport.requests.get(0).body;
    assertTrue(body.contains("grant_type=password"));
    assertTrue(body.contains("client_id=test-client-id"));
    assertTrue(body.contains("client_secret=test-client-secret"));
    assertTrue(body.contains("username=user%40example.com"));
    assertTrue(body.contains("password=pass123"));
  }

  @Test
  void passwordGrantParsesTokenResponse() {
    mockTransport.enqueueTokenResponse("my-access-token", "my-refresh-token", 7200);
    tokenManager = createTokenManager();

    tokenManager.getAccessToken();

    OAuthToken current = tokenManager.getCurrentToken();
    assertNotNull(current);
    assertEquals("my-access-token", current.getAccessToken());
    assertEquals("my-refresh-token", current.getRefreshToken());
    assertEquals(7200L, current.getExpiresIn());
    assertNotNull(current.getExpiresAt());
  }

  // ── Authorization code grant tests ────────────────────────────────────

  @Test
  void authorizationCodeGrantSendsCorrectParams() {
    credentialProvider =
        new CountingCredentialProvider(
            Credentials.builder()
                .clientCredentials(CLIENT_ID, CLIENT_SECRET)
                .authorizationCode("auth-code-xyz")
                .build());
    mockTransport.enqueueTokenResponse("access-1", "refresh-1", 3600);
    tokenManager = createTokenManager();

    tokenManager.getAccessToken();

    String body = mockTransport.requests.get(0).body;
    assertTrue(body.contains("grant_type=authorization_code"));
    assertTrue(body.contains("code=auth-code-xyz"));
    assertTrue(body.contains("client_id=test-client-id"));
    assertTrue(body.contains("client_secret=test-client-secret"));
    assertFalse(body.contains("username"));
    assertFalse(body.contains("password"));
  }

  @Test
  void authorizationCodeRedirectGrantValidatesHmacAndExtractsCode() {
    String redirectPath = "/oauth/callback";
    String queryWithoutH = "code=auth-code-xyz&state=abc";
    String signature =
        HmacValidator.computeUrlEncodedHmac(redirectPath + "?" + queryWithoutH, CLIENT_SECRET);
    credentialProvider =
        new CountingCredentialProvider(
            Credentials.builder()
                .clientCredentials(CLIENT_ID, CLIENT_SECRET)
                .authorizationCodeRedirectUri(
                    "https://example.com" + redirectPath + "?" + queryWithoutH + "&h=" + signature)
                .build());
    mockTransport.enqueueTokenResponse("access-1", "refresh-1", 3600);
    tokenManager = createTokenManager();

    tokenManager.getAccessToken();

    String body = mockTransport.requests.get(0).body;
    assertTrue(body.contains("grant_type=authorization_code"));
    assertTrue(body.contains("code=auth-code-xyz"));
  }

  @Test
  void authorizationCodeRedirectGrantRejectsInvalidHmacBeforeTokenExchange() {
    credentialProvider =
        new CountingCredentialProvider(
            Credentials.builder()
                .clientCredentials(CLIENT_ID, CLIENT_SECRET)
                .authorizationCodeRedirectUri(
                    "https://example.com/oauth/callback?code=auth-code-xyz&h=invalid")
                .build());
    tokenManager = createTokenManager();

    assertThrows(ShareFileAuthenticationException.class, tokenManager::getAccessToken);
    assertEquals(
        0, mockTransport.requests.size(), "No token exchange should happen on invalid HMAC");
  }

  @Test
  void authorizationCodeRedirectGrantRejectsMissingHmacBeforeTokenExchange() {
    credentialProvider =
        new CountingCredentialProvider(
            Credentials.builder()
                .clientCredentials(CLIENT_ID, CLIENT_SECRET)
                .authorizationCodeRedirectUri(
                    "https://example.com/oauth/callback?code=auth-code-xyz")
                .build());
    tokenManager = createTokenManager();

    assertThrows(ShareFileAuthenticationException.class, tokenManager::getAccessToken);
    assertEquals(0, mockTransport.requests.size(), "No token exchange should happen without HMAC");
  }

  // ── Token caching tests ───────────────────────────────────────────────

  @Test
  void secondCallReturnsCachedToken() {
    mockTransport.enqueueTokenResponse("access-1", "refresh-1", 3600);
    tokenManager = createTokenManager();

    String first = tokenManager.getAccessToken();
    String second = tokenManager.getAccessToken();

    assertEquals(first, second);
    assertEquals(1, mockTransport.requests.size(), "Should only make one HTTP request");
    assertEquals(1, credentialProvider.callCount.get(), "CredentialProvider called only once");
  }

  // ── Refresh token tests ───────────────────────────────────────────────

  @Test
  void refreshUsesRefreshTokenGrantWithoutCredentialProvider() {
    // Start with an expired token that has a refresh token
    OAuthToken expiredToken = new OAuthToken();
    expiredToken.setAccessToken("old-access");
    expiredToken.setRefreshToken("refresh-1");
    expiredToken.setExpiresIn(3600L);
    expiredToken.setExpiresAt(Instant.now().minusSeconds(60)); // already expired

    tokenStore.save(expiredToken);

    mockTransport.enqueueTokenResponse("new-access", "new-refresh", 3600);

    // Use a credential provider that should NOT be called
    credentialProvider = new CountingCredentialProvider();
    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER);

    String token = tokenManager.getAccessToken();

    assertEquals("new-access", token);
    assertEquals(1, mockTransport.requests.size());

    // Verify refresh_token grant was used
    String body = mockTransport.requests.get(0).body;
    assertTrue(body.contains("grant_type=refresh_token"));
    assertTrue(body.contains("refresh_token=refresh-1"));
    assertTrue(body.contains("client_id=test-client-id"));
    assertTrue(body.contains("client_secret=test-client-secret"));

    // CredentialProvider should NOT have been called for refresh
    assertEquals(
        0,
        credentialProvider.callCount.get(),
        "CredentialProvider must not be called during token refresh");
  }

  @Test
  void refreshFailureFallsBackToCredentialProvider() {
    // Start with an expired token
    OAuthToken expiredToken = new OAuthToken();
    expiredToken.setAccessToken("old-access");
    expiredToken.setRefreshToken("invalid-refresh");
    expiredToken.setExpiresIn(3600L);
    expiredToken.setExpiresAt(Instant.now().minusSeconds(60));

    tokenStore.save(expiredToken);

    // First 3 requests (refresh retries) fail, then full auth succeeds
    for (int i = 0; i < 3; i++) {
      mockTransport.enqueueErrorResponse(401, "{\"error\":\"invalid_grant\"}");
    }
    mockTransport.enqueueTokenResponse("fresh-access", "fresh-refresh", 3600);

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER);

    String token = tokenManager.getAccessToken();

    assertEquals("fresh-access", token);
    // 3 refresh retries + 1 full auth = 4 requests
    assertEquals(4, mockTransport.requests.size());
    // CredentialProvider should be called once for the full re-auth
    assertEquals(1, credentialProvider.callCount.get());

    // The last request should be a password grant
    String lastBody = mockTransport.requests.get(3).body;
    assertTrue(lastBody.contains("grant_type=password"));
  }

  // ── TokenStore persistence tests ──────────────────────────────────────

  @Test
  void tokenStoreSaveCalledAfterAcquisition() {
    mockTransport.enqueueTokenResponse("access-1", "refresh-1", 3600);
    tokenManager = createTokenManager();

    tokenManager.getAccessToken();

    assertTrue(tokenStore.load().isPresent());
    assertEquals("access-1", tokenStore.load().get().getAccessToken());
  }

  @Test
  void tokenStoreLoadCheckedOnStartup() {
    // Pre-populate store with a valid token
    OAuthToken cached = new OAuthToken();
    cached.setAccessToken("cached-access");
    cached.setRefreshToken("cached-refresh");
    cached.setExpiresIn(3600L);
    cached.setExpiresAt(Instant.now().plusSeconds(3600));
    tokenStore.save(cached);

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER);

    String token = tokenManager.getAccessToken();

    assertEquals("cached-access", token);
    assertEquals(0, mockTransport.requests.size(), "No HTTP request when cache is valid");
    assertEquals(0, credentialProvider.callCount.get());
  }

  @Test
  void expiredCachedTokenTriggersRefresh() {
    // Pre-populate store with an expired token
    OAuthToken expired = new OAuthToken();
    expired.setAccessToken("expired-access");
    expired.setRefreshToken("cached-refresh");
    expired.setExpiresIn(3600L);
    expired.setExpiresAt(Instant.now().minusSeconds(60));
    tokenStore.save(expired);

    mockTransport.enqueueTokenResponse("new-access", "new-refresh", 3600);

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER);

    String token = tokenManager.getAccessToken();

    assertEquals("new-access", token);
    // Should have used refresh_token grant, not called credential provider
    assertEquals(0, credentialProvider.callCount.get());
  }

  @Test
  void cachedTokenWithoutExpiresAtTriggersRefreshInsteadOfBeingTrustedForever() {
    OAuthToken persisted = new OAuthToken();
    persisted.setAccessToken("persisted-access");
    persisted.setRefreshToken("persisted-refresh");
    persisted.setExpiresIn(3600L);
    tokenStore.save(persisted);

    mockTransport.enqueueTokenResponse("refreshed-access", "refreshed-refresh", 3600);

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER);

    String token = tokenManager.getAccessToken();

    assertEquals("refreshed-access", token);
    assertEquals(1, mockTransport.requests.size());
    assertTrue(mockTransport.requests.get(0).body.contains("grant_type=refresh_token"));
  }

  // ── Thread safety tests ───────────────────────────────────────────────

  @Test
  void concurrentCallsDontDuplicateAuthRequests() throws InterruptedException {
    mockTransport.enqueueTokenResponse("access-1", "refresh-1", 3600);
    // Add a small delay to the transport to make the race window wider
    mockTransport.responseDelayMs = 50;
    tokenManager = createTokenManager();

    int threadCount = 10;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    List<String> tokens = new CopyOnWriteArrayList<>();
    List<Throwable> errors = new CopyOnWriteArrayList<>();

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              tokens.add(tokenManager.getAccessToken());
            } catch (Throwable t) {
              errors.add(t);
            } finally {
              doneLatch.countDown();
            }
          });
    }

    startLatch.countDown(); // Release all threads simultaneously
    assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
    executor.shutdown();

    assertTrue(errors.isEmpty(), "No errors expected: " + errors);
    assertEquals(threadCount, tokens.size());
    // All threads should get the same token
    tokens.forEach(t -> assertEquals("access-1", t));
    // Only one HTTP request should have been made
    assertEquals(1, mockTransport.requests.size());
  }

  // ── Error handling tests ──────────────────────────────────────────────

  @Test
  void tokenEndpointErrorThrowsAuthException() {
    mockTransport.enqueueErrorResponse(400, "{\"error\":\"invalid_request\"}");
    tokenManager = createTokenManager();

    ShareFileAuthenticationException ex =
        assertThrows(ShareFileAuthenticationException.class, tokenManager::getAccessToken);
    assertTrue(ex.getMessage().contains("400"));
  }

  @Test
  void credentialProviderFailureThrowsAuthException() {
    credentialProvider =
        new CountingCredentialProvider() {
          @Override
          public Credentials resolve() {
            callCount.incrementAndGet();
            throw new CredentialResolutionException("Vault unavailable");
          }
        };
    tokenManager = createTokenManager();

    ShareFileAuthenticationException ex =
        assertThrows(ShareFileAuthenticationException.class, tokenManager::getAccessToken);
    assertTrue(ex.getMessage().contains("resolve credentials"));
    assertInstanceOf(CredentialResolutionException.class, ex.getCause());
  }

  // ── Pre-existing token tests ──────────────────────────────────────────

  @Test
  void preExistingTokenUsedWithoutAuth() {
    OAuthToken preExisting = new OAuthToken();
    preExisting.setAccessToken("pre-existing-token");
    preExisting.setRefreshToken("pre-existing-refresh");
    preExisting.setExpiresIn(3600L);
    preExisting.computeExpiresAt();

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER,
            preExisting);

    String token = tokenManager.getAccessToken();

    assertEquals("pre-existing-token", token);
    assertEquals(0, mockTransport.requests.size());
    assertEquals(0, credentialProvider.callCount.get());
  }

  @Test
  void preExistingTokenWithoutExpiryIsTrustedInMemory() {
    OAuthToken preExisting = new OAuthToken();
    preExisting.setAccessToken("seeded-access-token");
    preExisting.setRefreshToken("seeded-refresh-token");

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER,
            preExisting);

    String token = tokenManager.getAccessToken();

    assertEquals("seeded-access-token", token);
    assertEquals(0, mockTransport.requests.size());
    assertEquals(0, credentialProvider.callCount.get());
  }

  @Test
  void refreshAccessTokenBypassesValidCachedToken() {
    OAuthToken preExisting = new OAuthToken();
    preExisting.setAccessToken("still-valid");
    preExisting.setRefreshToken("refresh-me");
    preExisting.setExpiresIn(3600L);
    preExisting.setExpiresAt(Instant.now().plusSeconds(3600));

    mockTransport.enqueueTokenResponse("new-access", "new-refresh", 3600);

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER,
            preExisting);

    String token = tokenManager.refreshAccessToken();

    assertEquals("new-access", token);
    assertEquals(1, mockTransport.requests.size());
    assertTrue(mockTransport.requests.get(0).body.contains("grant_type=refresh_token"));
  }

  @Test
  void reauthenticateWithCurrentSourceBypassesValidCachedToken() {
    OAuthToken preExisting = new OAuthToken();
    preExisting.setAccessToken("still-valid");
    preExisting.setRefreshToken("refresh-me");
    preExisting.setExpiresIn(3600L);
    preExisting.setExpiresAt(Instant.now().plusSeconds(3600));

    mockTransport.enqueueTokenResponse("new-access", "new-refresh", 3600);

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER,
            preExisting);

    String token = tokenManager.reauthenticateWithCurrentSource();

    assertEquals("new-access", token);
    assertEquals(1, mockTransport.requests.size());
    assertTrue(mockTransport.requests.get(0).body.contains("grant_type=refresh_token"));
  }

  @Test
  void reauthenticateWithCredentialsSwitchesToNewStaticCredentials() {
    OAuthToken preExisting = new OAuthToken();
    preExisting.setAccessToken("still-valid");
    preExisting.setRefreshToken("refresh-me");
    preExisting.setExpiresIn(3600L);
    preExisting.setExpiresAt(Instant.now().plusSeconds(3600));

    mockTransport.enqueueTokenResponse("rotated-access", "rotated-refresh", 3600);

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER,
            preExisting);

    Credentials rotatedCredentials =
        Credentials.builder()
            .clientCredentials("rotated-client-id", "rotated-client-secret")
            .passwordGrant("rotated-user@example.com", "rotated-pass")
            .build();

    String token = tokenManager.reauthenticateWithCredentials(rotatedCredentials);

    assertEquals("rotated-access", token);
    assertEquals(1, mockTransport.requests.size());
    String body = mockTransport.requests.get(0).body;
    assertTrue(body.contains("grant_type=password"));
    assertTrue(body.contains("client_id=rotated-client-id"));
    assertTrue(body.contains("client_secret=rotated-client-secret"));
    assertTrue(body.contains("username=rotated-user%40example.com"));
    assertTrue(body.contains("password=rotated-pass"));
    assertEquals(0, credentialProvider.callCount.get());
  }

  @Test
  void reauthenticateWithProviderSwitchesFutureFullAuthenticationSource() {
    OAuthToken preExisting = new OAuthToken();
    preExisting.setAccessToken("still-valid");
    preExisting.setRefreshToken("refresh-me");
    preExisting.setExpiresIn(3600L);
    preExisting.setExpiresAt(Instant.now().plusSeconds(3600));

    Credentials rotatedCredentials =
        Credentials.builder()
            .clientCredentials("rotated-client-id", "rotated-client-secret")
            .passwordGrant("rotated-user@example.com", "rotated-pass")
            .build();
    CountingCredentialProvider rotatedProvider = new CountingCredentialProvider(rotatedCredentials);

    mockTransport.enqueueTokenResponse("rotated-access", "rotated-refresh", 3600);
    mockTransport.enqueueErrorResponse(401, "{\"error\":\"invalid_grant\"}");
    mockTransport.enqueueErrorResponse(401, "{\"error\":\"invalid_grant\"}");
    mockTransport.enqueueErrorResponse(401, "{\"error\":\"invalid_grant\"}");
    mockTransport.enqueueTokenResponse("reauth-access", "reauth-refresh", 3600);

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER,
            preExisting);

    assertEquals("rotated-access", tokenManager.reauthenticateWithProvider(rotatedProvider));
    assertEquals("reauth-access", tokenManager.reauthenticateWithCurrentSource());

    assertEquals(5, mockTransport.requests.size());
    assertEquals(2, rotatedProvider.callCount.get());
    String lastBody = mockTransport.requests.get(4).body;
    assertTrue(lastBody.contains("grant_type=password"));
    assertTrue(lastBody.contains("client_id=rotated-client-id"));
    assertTrue(lastBody.contains("client_secret=rotated-client-secret"));
  }

  @Test
  void seededTokenOnlyReauthenticateWithCurrentSourceFailsWithoutFullAuthSource() {
    OAuthToken preExisting = new OAuthToken();
    preExisting.setAccessToken("still-valid");
    preExisting.setExpiresIn(3600L);
    preExisting.setExpiresAt(Instant.now().plusSeconds(3600));

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            () -> {
              throw new CredentialResolutionException("no full auth source");
            },
            mockTransport,
            tokenStore,
            OBJECT_MAPPER,
            preExisting);

    ShareFileAuthenticationException ex =
        assertThrows(
            ShareFileAuthenticationException.class, tokenManager::reauthenticateWithCurrentSource);
    assertTrue(ex.getMessage().contains("resolve credentials"));
  }

  @Test
  void tokenRefreshBufferCanScheduleEarlierThanEightyPercent() throws Exception {
    config =
        ShareFileConfig.builder()
            .subdomain("testcompany")
            .tokenRefreshBuffer(Duration.ofSeconds(30))
            .build();

    OAuthToken preExisting = new OAuthToken();
    preExisting.setAccessToken("buffer-token");
    preExisting.setRefreshToken("buffer-refresh");
    preExisting.setExpiresIn(100L);
    preExisting.setExpiresAt(Instant.now().plusSeconds(100));

    tokenManager =
        new TokenManager(
            config,
            CLIENT_ID,
            CLIENT_SECRET,
            credentialProvider,
            mockTransport,
            tokenStore,
            OBJECT_MAPPER,
            preExisting);

    ScheduledFuture<?> scheduledRefresh = scheduledRefresh(tokenManager);

    long delaySeconds = scheduledRefresh.getDelay(TimeUnit.SECONDS);
    assertTrue(delaySeconds <= 70, "Expected refresh delay to honor 30s buffer");
    assertTrue(delaySeconds >= 67, "Unexpectedly short delay: " + delaySeconds);
  }

  @Test
  void dynamicProviderCanSupplyClientCredentialsForRefreshWhenNotSeededAtConstruction() {
    OAuthToken expired = new OAuthToken();
    expired.setAccessToken("expired-access");
    expired.setRefreshToken("cached-refresh");
    expired.setExpiresIn(3600L);
    expired.setExpiresAt(Instant.now().minusSeconds(60));
    tokenStore.save(expired);

    mockTransport.enqueueTokenResponse("new-access", "new-refresh", 3600);

    tokenManager =
        new TokenManager(
            config, credentialProvider, mockTransport, tokenStore, OBJECT_MAPPER, expired);

    String token = tokenManager.getAccessToken();

    assertEquals("new-access", token);
    assertEquals(1, credentialProvider.callCount.get());
    assertEquals(1, mockTransport.requests.size());
    assertTrue(mockTransport.requests.get(0).body.contains("grant_type=refresh_token"));
    assertTrue(mockTransport.requests.get(0).body.contains("client_id=test-client-id"));
    assertTrue(mockTransport.requests.get(0).body.contains("client_secret=test-client-secret"));
  }

  // ── Close/lifecycle tests ─────────────────────────────────────────────

  @Test
  void closeDoesNotThrow() {
    mockTransport.enqueueTokenResponse("access-1", "refresh-1", 3600);
    tokenManager = createTokenManager();
    tokenManager.getAccessToken();

    assertDoesNotThrow(() -> tokenManager.close());
  }

  @Test
  void tokenEndpointUrlUsesConfig() {
    config = ShareFileConfig.builder().subdomain("myco").apiControlPlane("securevdr.com").build();
    mockTransport.enqueueTokenResponse("access-1", "refresh-1", 3600);
    tokenManager = createTokenManager();

    tokenManager.getAccessToken();

    URI requestUri = mockTransport.requests.get(0).uri;
    assertEquals("https://myco.securevdr.com/oauth/token", requestUri.toString());
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private TokenManager createTokenManager() {
    return new TokenManager(
        config,
        CLIENT_ID,
        CLIENT_SECRET,
        credentialProvider,
        mockTransport,
        tokenStore,
        OBJECT_MAPPER);
  }

  private static ScheduledFuture<?> scheduledRefresh(TokenManager tokenManager) throws Exception {
    var field = TokenManager.class.getDeclaredField("scheduledRefresh");
    field.setAccessible(true);
    return (ScheduledFuture<?>) field.get(tokenManager);
  }

  /**
   * A CredentialProvider that counts how many times resolve() is called, to verify that it's NOT
   * called during normal token refresh.
   */
  private static class CountingCredentialProvider implements CredentialProvider {
    final AtomicInteger callCount = new AtomicInteger();
    private final Credentials credentials;

    CountingCredentialProvider() {
      this(
          Credentials.builder()
              .clientCredentials(CLIENT_ID, CLIENT_SECRET)
              .passwordGrant("user@example.com", "pass123")
              .build());
    }

    CountingCredentialProvider(Credentials credentials) {
      this.credentials = credentials;
    }

    @Override
    public Credentials resolve() {
      callCount.incrementAndGet();
      return credentials;
    }
  }

  /** Records HTTP requests and returns enqueued responses. */
  private static class MockTransport implements HttpTransport {
    final List<RecordedRequest> requests = Collections.synchronizedList(new ArrayList<>());
    private final List<MockResponse> responses = Collections.synchronizedList(new ArrayList<>());
    volatile long responseDelayMs = 0;

    void enqueueTokenResponse(String accessToken, String refreshToken, long expiresIn) {
      String json =
          """
          {
            "access_token": "%s",
            "refresh_token": "%s",
            "token_type": "bearer",
            "expires_in": %d
          }
          """
              .formatted(accessToken, refreshToken, expiresIn);
      responses.add(new MockResponse(200, json));
    }

    void enqueueErrorResponse(int statusCode, String body) {
      responses.add(new MockResponse(statusCode, body));
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
      // Record the request
      String body =
          request
              .bodyStream()
              .map(
                  stream -> {
                    try {
                      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                      return "";
                    }
                  })
              .orElse("");
      requests.add(new RecordedRequest(request.uri(), request.method(), body, request.headers()));

      if (responseDelayMs > 0) {
        try {
          Thread.sleep(responseDelayMs);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      if (responses.isEmpty()) {
        throw new RuntimeException("No mock responses enqueued");
      }
      MockResponse mockResp = responses.remove(0);

      byte[] respBytes = mockResp.body.getBytes(StandardCharsets.UTF_8);
      return new HttpResponse() {
        private final InputStream stream = new ByteArrayInputStream(respBytes);

        @Override
        public int statusCode() {
          return mockResp.statusCode;
        }

        @Override
        public Map<String, List<String>> headers() {
          return Map.of();
        }

        @Override
        public InputStream bodyStream() {
          return stream;
        }

        @Override
        public byte[] bodyBytes() {
          return respBytes;
        }

        @Override
        public byte[] bodyBytes(int maxBytes) {
          if (respBytes.length > maxBytes) {
            throw new RuntimeException("Response exceeds max size");
          }
          return respBytes;
        }

        @Override
        public void close() {
          // no-op
        }
      };
    }

    record RecordedRequest(URI uri, String method, String body, Map<String, String> headers) {}

    record MockResponse(int statusCode, String body) {}
  }
}
