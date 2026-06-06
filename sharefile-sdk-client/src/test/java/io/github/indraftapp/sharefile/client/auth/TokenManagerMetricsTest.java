package io.github.indraftapp.sharefile.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.MetricNames;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.internal.RecordingMetricsProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import io.github.indraftapp.sharefile.core.jackson.ShareFileObjectMapper;
import io.github.indraftapp.sharefile.core.model.OAuthToken;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TokenManagerMetricsTest {

  private static final ObjectMapper MAPPER = ShareFileObjectMapper.create();

  @Test
  void refreshEmitsSuccessCounterAndExpiryGauge() {
    TestTransport transport = new TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "access_token": "fresh-token",
          "refresh_token": "fresh-refresh",
          "token_type": "bearer",
          "expires_in": 3600
        }
        """);
    RecordingMetricsProvider metrics = new RecordingMetricsProvider();
    OAuthToken expired = new OAuthToken();
    expired.setAccessToken("stale-token");
    expired.setRefreshToken("stale-refresh");
    expired.setExpiresIn(3600L);
    expired.setExpiresAt(Instant.now().minusSeconds(30));

    try (TokenManager tokenManager =
        new TokenManager(
            ShareFileConfig.builder().subdomain("testco").build(),
            "client-id",
            "client-secret",
            () ->
                Credentials.builder()
                    .clientCredentials("client-id", "client-secret")
                    .passwordGrant("user", "pass")
                    .build(),
            transport,
            new InMemoryTokenStore(),
            MAPPER,
            metrics,
            expired)) {
      String token = tokenManager.getAccessToken();

      assertEquals("fresh-token", token);
      assertTrue(metrics.counters.contains(MetricNames.AUTH_TOKEN_REFRESH));
      assertTrue(metrics.gauges.contains(MetricNames.AUTH_TOKEN_EXPIRY));
    }
  }

  private static final class TestTransport implements HttpTransport {
    private final List<String> responses = new ArrayList<>();

    void enqueueJsonResponse(int statusCode, String body) {
      responses.add(body);
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
      String responseBody = responses.remove(0);
      byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
      return new HttpResponse() {
        @Override
        public int statusCode() {
          return 200;
        }

        @Override
        public Map<String, List<String>> headers() {
          return Map.of();
        }

        @Override
        public InputStream bodyStream() {
          return new ByteArrayInputStream(bytes);
        }

        @Override
        public byte[] bodyBytes() throws ShareFileNetworkException {
          return bytes;
        }

        @Override
        public byte[] bodyBytes(int maxBytes) throws ShareFileNetworkException {
          return bytes;
        }

        @Override
        public void close() {}
      };
    }
  }
}
