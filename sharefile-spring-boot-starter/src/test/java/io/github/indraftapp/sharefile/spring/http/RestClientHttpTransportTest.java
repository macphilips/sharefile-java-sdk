package io.github.indraftapp.sharefile.spring.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestClientHttpTransportTest {

  @Test
  void bodylessRequestMapsResponseAndNormalizesHeaders() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).bufferContent().build();
    server.expect(once(), requestTo("https://example.test/items"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess("ok", MediaType.TEXT_PLAIN)
                .header("X-Custom-Header", "value"));

    RestClientHttpTransport transport = new RestClientHttpTransport(builder.build());
    HttpTransport.HttpResponse response =
        transport.execute(
            request(
                "GET",
                URI.create("https://example.test/items"),
                Map.of("Accept", "text/plain"),
                null,
                OptionalLong.empty()));

    try (response) {
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(new String(response.bodyBytes(), StandardCharsets.UTF_8)).isEqualTo("ok");
      assertThat(response.headers()).containsKey("x-custom-header");
    }
    server.verify();
  }

  @Test
  void jsonBodyIsForwarded() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).bufferContent().build();
    server.expect(once(), requestTo("https://example.test/items"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Content-Type", "application/json"))
        .andExpect(content().json("{\"name\":\"demo\"}"))
        .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

    RestClientHttpTransport transport = new RestClientHttpTransport(builder.build());
    try (HttpTransport.HttpResponse response =
        transport.execute(
            request(
                "POST",
                URI.create("https://example.test/items"),
                Map.of("Content-Type", "application/json", "Accept", "application/json"),
                "{\"name\":\"demo\"}",
                OptionalLong.of("{\"name\":\"demo\"}".getBytes(StandardCharsets.UTF_8).length)))) {
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(new String(response.bodyBytes(), StandardCharsets.UTF_8))
          .isEqualTo("{\"ok\":true}");
    }

    server.verify();
  }

  @Test
  void streamingBodyIsForwarded() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).bufferContent().build();
    server.expect(once(), requestTo("https://example.test/upload"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().string("streamed"))
        .andRespond(withSuccess("done", MediaType.TEXT_PLAIN));

    RestClientHttpTransport transport = new RestClientHttpTransport(builder.build());
    try (HttpTransport.HttpResponse response =
        transport.execute(
            request(
                "PUT",
                URI.create("https://example.test/upload"),
                Map.of("Accept", "text/plain"),
                "streamed",
                OptionalLong.empty()))) {
      assertThat(new String(response.bodyBytes(), StandardCharsets.UTF_8)).isEqualTo("done");
    }

    server.verify();
  }

  @Test
  void patchBodyIsForwarded() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).bufferContent().build();
    server.expect(once(), requestTo("https://example.test/items(fi1)"))
        .andExpect(method(HttpMethod.PATCH))
        .andExpect(content().json("{\"Name\":\"updated\"}"))
        .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

    RestClientHttpTransport transport = new RestClientHttpTransport(builder.build());
    try (HttpTransport.HttpResponse response =
        transport.execute(
            request(
                "PATCH",
                URI.create("https://example.test/items(fi1)"),
                Map.of("Content-Type", "application/json"),
                "{\"Name\":\"updated\"}",
                OptionalLong.of("{\"Name\":\"updated\"}".getBytes(StandardCharsets.UTF_8).length)))) {
      assertThat(response.statusCode()).isEqualTo(200);
    }

    server.verify();
  }

  @Test
  void transportExceptionsAreWrapped() {
    RestClient restClient =
        RestClient.builder()
            .requestFactory(
                new ClientHttpRequestFactory() {
                  @Override
                  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
                    throw new RuntimeException("factory boom");
                  }
                })
            .build();

    RestClientHttpTransport transport = new RestClientHttpTransport(restClient);

    assertThatThrownBy(
            () ->
                transport.execute(
                    request(
                        "GET",
                        URI.create("https://example.test/fail"),
                        Map.of(),
                        null,
                        OptionalLong.empty())))
        .isInstanceOf(ShareFileNetworkException.class)
        .hasMessageContaining("GET https://example.test/fail failed");
  }

  private static HttpTransport.HttpRequest request(
      String method,
      URI uri,
      Map<String, String> headers,
      String body,
      OptionalLong contentLength) {
    return new HttpTransport.HttpRequest() {
      @Override
      public String method() {
        return method;
      }

      @Override
      public URI uri() {
        return uri;
      }

      @Override
      public Map<String, String> headers() {
        return headers;
      }

      @Override
      public Optional<InputStream> bodyStream() {
        if (body == null) {
          return Optional.empty();
        }
        return Optional.of(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
      }

      @Override
      public OptionalLong contentLength() {
        return contentLength;
      }

      @Override
      public Duration timeout() {
        return Duration.ofSeconds(30);
      }
    };
  }
}
