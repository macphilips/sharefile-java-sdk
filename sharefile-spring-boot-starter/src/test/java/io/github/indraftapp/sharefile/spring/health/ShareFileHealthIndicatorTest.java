package io.github.indraftapp.sharefile.spring.health;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.indraftapp.sharefile.client.ShareFileClient;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShareFileHealthIndicatorTest {

  @Test
  void healthReturnsUpWithDetails() {
    ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(new JsonTransport(200, "{\"Subdomain\":\"healthco\"}"))
            .build();
    try {
      ShareFileHealthIndicator indicator = new ShareFileHealthIndicator(client);

      org.springframework.boot.actuate.health.Health health = indicator.health();

      assertThat(health.getStatus().getCode()).isEqualTo("UP");
      assertThat(health.getDetails()).containsEntry("subdomain", "healthco");
      assertThat(health.getDetails()).containsKey("tokenExpiresIn");
    } finally {
      client.close();
    }
  }

  @Test
  void healthReturnsDownWithError() {
    ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(request -> {
              throw new IllegalStateException("boom");
            })
            .build();
    try {
      ShareFileHealthIndicator indicator = new ShareFileHealthIndicator(client);

      org.springframework.boot.actuate.health.Health health = indicator.health();

      assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
      assertThat(health.getDetails()).containsEntry("error", "boom");
    } finally {
      client.close();
    }
  }

  private static final class JsonTransport implements HttpTransport {
    private final int status;
    private final byte[] body;

    private JsonTransport(int status, String body) {
      this.status = status;
      this.body = body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
      return new HttpResponse() {
        private final InputStream stream = new ByteArrayInputStream(body);

        @Override
        public int statusCode() {
          return status;
        }

        @Override
        public Map<String, List<String>> headers() {
          return Map.of("content-type", List.of("application/json"));
        }

        @Override
        public InputStream bodyStream() {
          return stream;
        }

        @Override
        public byte[] bodyBytes() {
          return body;
        }

        @Override
        public byte[] bodyBytes(int maxBytes) {
          return body;
        }

        @Override
        public void close() {}
      };
    }
  }
}
