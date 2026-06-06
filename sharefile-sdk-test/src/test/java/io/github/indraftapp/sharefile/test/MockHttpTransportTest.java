package io.github.indraftapp.sharefile.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.indraftapp.sharefile.client.http.HttpTransport;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class MockHttpTransportTest {

  @Test
  void recordsRequestsAndReturnsQueuedJsonResponse() {
    MockHttpTransport transport = new MockHttpTransport();
    transport.enqueueJsonResponse(200, "{\"ok\":true}");

    HttpTransport.HttpResponse response =
        transport.execute(
            new TestRequest(
                "POST", URI.create("https://example.test/items"), "{\"name\":\"Doc\"}"));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(new String(response.bodyBytes(), java.nio.charset.StandardCharsets.UTF_8))
        .isEqualTo("{\"ok\":true}");
    assertThat(transport.getLastRequest().method()).isEqualTo("POST");
    assertThat(transport.getLastRequest().uri())
        .isEqualTo(URI.create("https://example.test/items"));
    assertThat(transport.getLastRequest().body()).isEqualTo("{\"name\":\"Doc\"}");
  }

  @Test
  void supportsStreamingResponsesAndRequestHistory() throws Exception {
    MockHttpTransport transport = new MockHttpTransport();
    transport.enqueueStreamResponse(200, new ByteArrayInputStream("stream-body".getBytes()));
    transport.enqueueBodylessResponse(204);

    HttpTransport.HttpResponse first =
        transport.execute(new TestRequest("GET", URI.create("https://example.test/files"), null));
    HttpTransport.HttpResponse second =
        transport.execute(
            new TestRequest("DELETE", URI.create("https://example.test/files/1"), null));

    assertThat(
            new String(first.bodyStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
        .isEqualTo("stream-body");
    assertThat(second.statusCode()).isEqualTo(204);
    assertThat(transport.getRequests()).hasSize(2);
    assertThat(transport.pendingResponses()).isZero();
  }

  private record TestRequest(String method, URI uri, String body)
      implements HttpTransport.HttpRequest {

    @Override
    public Map<String, String> headers() {
      return Map.of("content-type", "application/json");
    }

    @Override
    public Optional<java.io.InputStream> bodyStream() {
      return body == null
          ? Optional.empty()
          : Optional.of(
              new ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Override
    public OptionalLong contentLength() {
      return body == null
          ? OptionalLong.empty()
          : OptionalLong.of(body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }

    @Override
    public Duration timeout() {
      return Duration.ofSeconds(5);
    }
  }
}
