package io.github.indraftapp.sharefile.client.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.auth.InMemoryTokenStore;
import io.github.indraftapp.sharefile.client.auth.TokenManager;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.core.exception.ShareFileApiException;
import io.github.indraftapp.sharefile.core.exception.ShareFileBadRequestException;
import io.github.indraftapp.sharefile.core.exception.ShareFileConflictException;
import io.github.indraftapp.sharefile.core.exception.ShareFileForbiddenException;
import io.github.indraftapp.sharefile.core.exception.ShareFileNotFoundException;
import io.github.indraftapp.sharefile.core.exception.ShareFileRateLimitException;
import io.github.indraftapp.sharefile.core.exception.ShareFileServerException;
import io.github.indraftapp.sharefile.core.exception.ShareFileUnauthorizedException;
import io.github.indraftapp.sharefile.core.jackson.ShareFileObjectMapper;
import io.github.indraftapp.sharefile.core.model.OAuthToken;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link ShareFileHttpClient}. */
class ShareFileHttpClientTest {

  private static final String BASE_URL = "https://testco.sf-api.com/sf/v3";
  private static final ObjectMapper MAPPER = ShareFileObjectMapper.create();

  private MockTransport mockTransport;
  private io.github.indraftapp.sharefile.client.auth.TokenManager stubTokenManager;
  private RecordingMetricsProvider metricsProvider;
  private ShareFileHttpClient httpClient;

  @BeforeEach
  void setUp() {
    mockTransport = new MockTransport();
    stubTokenManager = StubTokenManager.create("test-bearer-token");
    metricsProvider = new RecordingMetricsProvider();
    httpClient = createClient(RetryConfig.builder().maxRetries(0).build());
  }

  // ── Bearer token & request ID tests ───────────────────────────────────

  @Test
  void getIncludesBearerTokenAndRequestId() {
    mockTransport.enqueueJsonResponse(200, "{\"Name\":\"test\"}");

    httpClient.get("/Items(abc)", Map.of(), TestItem.class);

    MockTransport.RecordedRequest req = mockTransport.getLastRequest();
    assertEquals("GET", req.method());
    assertTrue(req.headers().containsKey("Authorization"));
    assertEquals("Bearer test-bearer-token", req.headers().get("Authorization"));
    assertTrue(req.headers().containsKey("X-Request-Id"));
    assertNotNull(req.headers().get("X-Request-Id"));
  }

  @Test
  void getBuildsCorrectUri() {
    mockTransport.enqueueJsonResponse(200, "{\"Name\":\"home\"}");

    httpClient.get("/Items(home)", Map.of("$select", "Name,Id"), TestItem.class);

    MockTransport.RecordedRequest req = mockTransport.getLastRequest();
    String uri = req.uri().toString();
    assertTrue(uri.startsWith(BASE_URL + "/Items(home)"));
    assertTrue(uri.contains("%24select=Name%2CId"));
  }

  @Test
  void getEncodesReservedCharactersInQueryValues() {
    mockTransport.enqueueJsonResponse(200, "{\"Name\":\"home\"}");

    httpClient.get("/Items", Map.of("$filter", "Name eq 'A&B + 100%'"), TestItem.class);

    String uri = mockTransport.getLastRequest().uri().toString();
    assertTrue(uri.contains("%24filter="));
    assertTrue(uri.contains("%27A%26B%20%2B%20100%25%27"));
  }

  // ── POST serialization tests ──────────────────────────────────────────

  @Test
  void postSerializesBodyToJson() {
    mockTransport.enqueueJsonResponse(200, "{\"Name\":\"New Folder\"}");

    TestItem body = new TestItem();
    body.name = "New Folder";
    httpClient.post("/Items", body, TestItem.class);

    MockTransport.RecordedRequest req = mockTransport.getLastRequest();
    assertEquals("POST", req.method());
    assertEquals("application/json", req.headers().get("Content-Type"));
    assertTrue(req.body().contains("\"Name\""));
    assertTrue(req.body().contains("New Folder"));
  }

  // ── Error mapping tests ───────────────────────────────────────────────

  @Test
  void status404ThrowsNotFoundException() {
    mockTransport.enqueueJsonResponse(
        404,
        """
        {"code":"NotFound","message":{"lang":"en-US","value":"The item was not found."}}
        """);

    ShareFileNotFoundException ex =
        assertThrows(
            ShareFileNotFoundException.class,
            () -> httpClient.get("/Items(missing)", Map.of(), TestItem.class));

    assertEquals(404, ex.getHttpStatus());
    assertEquals("NotFound", ex.getErrorCode());
    assertEquals("The item was not found.", ex.getErrorMessage());
    assertNotNull(ex.getRequestId());
    assertEquals("GET", ex.getRequestMethod());
  }

  @Test
  void status400ThrowsBadRequestException() {
    mockTransport.enqueueJsonResponse(
        400,
        """
        {"code":"BadRequest","message":{"lang":"en-US","value":"Invalid parameters"}}
        """);

    assertInstanceOf(
        ShareFileBadRequestException.class,
        assertThrows(
            ShareFileApiException.class, () -> httpClient.get("/Items", Map.of(), TestItem.class)));
  }

  @Test
  void status401ThrowsUnauthorizedException() {
    // With maxRetries=0, 401 retry is still attempted once internally
    mockTransport.enqueueJsonResponse(
        401, "{\"code\":\"Unauthorized\",\"message\":{\"value\":\"Invalid token\"}}");
    // After token refresh, still 401
    mockTransport.enqueueJsonResponse(
        401, "{\"code\":\"Unauthorized\",\"message\":{\"value\":\"Invalid token\"}}");

    assertInstanceOf(
        ShareFileUnauthorizedException.class,
        assertThrows(
            ShareFileApiException.class, () -> httpClient.get("/Items", Map.of(), TestItem.class)));
  }

  @Test
  void status401RefreshesTokenAndRetriesWithNewBearerToken() {
    OAuthToken initialToken = new OAuthToken();
    initialToken.setAccessToken("stale-token");
    initialToken.setRefreshToken("refresh-token");
    initialToken.setExpiresIn(3600L);
    initialToken.setExpiresAt(Instant.now().plusSeconds(3600));

    TokenManager refreshingTokenManager =
        new TokenManager(
            ShareFileConfig.builder().subdomain("testco").build(),
            "test-client-id",
            "test-client-secret",
            () ->
                Credentials.builder()
                    .clientCredentials("test-client-id", "test-client-secret")
                    .passwordGrant("user", "pass")
                    .build(),
            mockTransport,
            new InMemoryTokenStore(),
            MAPPER,
            initialToken);

    httpClient =
        new ShareFileHttpClient(
            mockTransport,
            refreshingTokenManager,
            MAPPER,
            RetryConfig.builder().maxRetries(0).build(),
            metricsProvider,
            BASE_URL,
            Duration.ofSeconds(30));

    mockTransport.enqueueJsonResponse(
        401, "{\"code\":\"Unauthorized\",\"message\":{\"value\":\"Invalid token\"}}");
    mockTransport.enqueueJsonResponse(
        200,
        """
        {
          "access_token": "fresh-token",
          "refresh_token": "fresh-refresh",
          "token_type": "bearer",
          "expires_in": 3600
        }
        """);
    mockTransport.enqueueJsonResponse(200, "{\"Name\":\"retried\"}");

    try {
      TestItem item = httpClient.get("/Items(abc)", Map.of(), TestItem.class);

      assertEquals("retried", item.name);
      assertEquals(3, mockTransport.requests.size());
      assertEquals(
          "Bearer stale-token", mockTransport.requests.get(0).headers().get("Authorization"));
      assertTrue(mockTransport.requests.get(1).uri().toString().endsWith("/oauth/token"));
      assertTrue(mockTransport.requests.get(1).body().contains("grant_type=refresh_token"));
      assertEquals(
          "Bearer fresh-token", mockTransport.requests.get(2).headers().get("Authorization"));
    } finally {
      refreshingTokenManager.close();
    }
  }

  @Test
  void status403ThrowsForbiddenException() {
    mockTransport.enqueueJsonResponse(
        403,
        """
        {"code":"Forbidden","message":{"value":"Access denied"}}
        """);

    assertInstanceOf(
        ShareFileForbiddenException.class,
        assertThrows(
            ShareFileApiException.class, () -> httpClient.get("/Items", Map.of(), TestItem.class)));
  }

  @Test
  void status409ThrowsConflictException() {
    mockTransport.enqueueJsonResponse(
        409,
        """
        {"code":"Conflict","message":{"value":"Already exists"}}
        """);

    assertInstanceOf(
        ShareFileConflictException.class,
        assertThrows(
            ShareFileApiException.class, () -> httpClient.get("/Items", Map.of(), TestItem.class)));
  }

  @Test
  void status500ThrowsServerException() {
    mockTransport.enqueueJsonResponse(
        500,
        """
        {"code":"InternalError","message":{"value":"Server error"}}
        """);

    assertInstanceOf(
        ShareFileServerException.class,
        assertThrows(
            ShareFileApiException.class, () -> httpClient.get("/Items", Map.of(), TestItem.class)));
  }

  @Test
  void status429ThrowsRateLimitException() {
    mockTransport.enqueueJsonResponse(
        429, "{\"code\":\"TooManyRequests\",\"message\":{\"value\":\"Slow down\"}}");

    ShareFileRateLimitException ex =
        assertThrows(
            ShareFileRateLimitException.class,
            () -> httpClient.get("/Items", Map.of(), TestItem.class));
    assertEquals(429, ex.getHttpStatus());
  }

  // ── 204 No Content handling ───────────────────────────────────────────

  @Test
  void status204ReturnsNull() {
    mockTransport.enqueueJsonResponse(204, "");

    Object result =
        httpClient.execute(
            "DELETE", "/Items(abc)", null, Map.of(), Void.class, RetryPolicy.DEFAULT);

    assertNull(result);
  }

  // ── Metrics tests ─────────────────────────────────────────────────────

  @Test
  void metricsRecordedOnSuccess() {
    mockTransport.enqueueJsonResponse(200, "{\"Name\":\"test\"}");

    httpClient.get("/Items(abc)", Map.of(), TestItem.class);

    assertTrue(metricsProvider.requestRecorded);
  }

  @Test
  void metricsRecordedOnError() {
    mockTransport.enqueueJsonResponse(
        404, "{\"code\":\"NotFound\",\"message\":{\"value\":\"Not found\"}}");

    assertThrows(
        ShareFileNotFoundException.class,
        () -> httpClient.get("/Items(abc)", Map.of(), TestItem.class));

    assertTrue(metricsProvider.errorRecorded);
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private ShareFileHttpClient createClient(RetryConfig retryConfig) {
    return new ShareFileHttpClient(
        mockTransport,
        stubTokenManager,
        MAPPER,
        retryConfig,
        metricsProvider,
        BASE_URL,
        Duration.ofSeconds(30));
  }

  /** Simple test DTO. */
  static class TestItem {
    @JsonProperty("Name")
    String name;
  }
}
