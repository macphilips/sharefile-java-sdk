package io.github.indraftapp.sharefile.spring.http;

import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

/**
 * Spring {@link RestClient}-backed {@link HttpTransport}.
 *
 * <p>This adapter delegates request execution to Spring's HTTP stack so Boot applications inherit
 * interceptors, observations, and request-factory behavior configured on the provided
 * {@link RestClient}. Per-request full-stream timeout enforcement remains the responsibility of the
 * underlying Spring client/request factory.
 */
public final class RestClientHttpTransport implements HttpTransport {

  private static final int BODY_CHUNK_SIZE = 8192;

  private final RestClient restClient;

  public RestClientHttpTransport(RestClient restClient) {
    this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
  }

  @Override
  public HttpResponse execute(HttpRequest request) throws ShareFileNetworkException {
    Objects.requireNonNull(request, "request must not be null");
    try {
      RestClient.RequestBodySpec requestSpec =
          restClient.method(HttpMethod.valueOf(request.method())).uri(request.uri());
      requestSpec.headers(
          headers -> request.headers().forEach((name, value) -> headers.add(name, value)));

      request.contentLength().ifPresent(requestSpec::contentLength);

      InputStream requestBody = request.bodyStream().orElse(null);
      if (requestBody != null) {
        requestSpec.body(
            (StreamingHttpOutputMessage.Body)
                outputStream -> {
                  try (InputStream stream = requestBody) {
                    stream.transferTo(outputStream);
                  }
                });
      }

      return requestSpec.exchange(
          (clientRequest, clientResponse) -> new RestClientResponse(clientResponse), false);
    } catch (RuntimeException e) {
      throw new ShareFileNetworkException(
          "%s %s failed: %s"
              .formatted(request.method(), request.uri(), safeMessage(e)),
          e);
    }
  }

  private static String safeMessage(Exception exception) {
    String message = exception.getMessage();
    return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
  }

  private static final class RestClientResponse implements HttpResponse {

    private final ClientHttpResponse delegate;
    private final Map<String, List<String>> headers;

    private RestClientResponse(ClientHttpResponse delegate) {
      this.delegate = delegate;
      Map<String, List<String>> normalized = new LinkedHashMap<>();
      delegate
          .getHeaders()
          .forEach(
              (name, values) ->
                  normalized.put(name.toLowerCase(Locale.ROOT), List.copyOf(values)));
      this.headers = Map.copyOf(normalized);
    }

    @Override
    public int statusCode() {
      try {
        return delegate.getStatusCode().value();
      } catch (IOException e) {
        throw new ShareFileNetworkException("Failed to read HTTP status code", e);
      }
    }

    @Override
    public Map<String, List<String>> headers() {
      return headers;
    }

    @Override
    public InputStream bodyStream() {
      try {
        return delegate.getBody();
      } catch (IOException e) {
        throw new ShareFileNetworkException("Failed to obtain response body stream", e);
      }
    }

    @Override
    public byte[] bodyBytes() {
      try (InputStream stream = bodyStream()) {
        return stream.readAllBytes();
      } catch (IOException e) {
        throw new ShareFileNetworkException("Failed to read response body", e);
      }
    }

    @Override
    public byte[] bodyBytes(int maxBytes) {
      if (maxBytes <= 0) {
        throw new IllegalArgumentException("maxBytes must be positive: " + maxBytes);
      }
      try (InputStream stream = bodyStream()) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        byte[] chunk = new byte[BODY_CHUNK_SIZE];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = stream.read(chunk)) != -1) {
          totalRead += bytesRead;
          if (totalRead > maxBytes) {
            throw new ShareFileNetworkException(
                "Response body exceeds maximum size of %d bytes".formatted(maxBytes));
          }
          buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
      } catch (IOException e) {
        throw new ShareFileNetworkException("Failed to read response body", e);
      }
    }

    @Override
    public void close() {
      try {
        delegate.close();
      } catch (Exception e) {
        throw new ShareFileNetworkException("Failed to close response body", e);
      }
    }
  }
}
