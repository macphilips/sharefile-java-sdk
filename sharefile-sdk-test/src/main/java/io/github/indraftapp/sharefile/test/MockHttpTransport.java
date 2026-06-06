package io.github.indraftapp.sharefile.test;

import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public test transport for enqueuing deterministic HTTP responses and recording executed requests.
 */
public final class MockHttpTransport implements HttpTransport {

  private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
  private final List<MockResponse> responses = Collections.synchronizedList(new ArrayList<>());
  private volatile ShareFileNetworkException connectionException;

  public void enqueueJsonResponse(int statusCode, String body) {
    enqueueJsonResponse(statusCode, body, Map.of());
  }

  public void enqueueJsonResponse(int statusCode, String body, Map<String, List<String>> headers) {
    enqueueResponse(statusCode, body.getBytes(StandardCharsets.UTF_8), headers);
  }

  public void enqueueResponse(int statusCode, byte[] body) {
    enqueueResponse(statusCode, body, Map.of());
  }

  public void enqueueResponse(int statusCode, byte[] body, Map<String, List<String>> headers) {
    responses.add(new MockResponse(statusCode, Objects.requireNonNull(body), Map.copyOf(headers)));
  }

  public void enqueueStreamResponse(int statusCode, InputStream bodyStream) {
    enqueueStreamResponse(statusCode, bodyStream, Map.of());
  }

  public void enqueueStreamResponse(
      int statusCode, InputStream bodyStream, Map<String, List<String>> headers) {
    try (InputStream stream = Objects.requireNonNull(bodyStream, "bodyStream must not be null")) {
      enqueueResponse(statusCode, stream.readAllBytes(), headers);
    } catch (IOException e) {
      throw new ShareFileNetworkException("Failed to buffer mock response stream", e);
    }
  }

  public void enqueueBodylessResponse(int statusCode) {
    enqueueResponse(statusCode, new byte[0]);
  }

  public List<RecordedRequest> getRequests() {
    return List.copyOf(requests);
  }

  public RecordedRequest getLastRequest() {
    return requests.get(requests.size() - 1);
  }

  public int pendingResponses() {
    return responses.size();
  }

  public void setConnectionException(ShareFileNetworkException exception) {
    this.connectionException = exception;
  }

  @Override
  public HttpResponse execute(HttpRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    byte[] requestBody = readRequestBody(request);
    requests.add(
        new RecordedRequest(
            request.uri(),
            request.method(),
            new String(requestBody, StandardCharsets.UTF_8),
            requestBody,
            Map.copyOf(request.headers()),
            request.timeout()));

    if (connectionException != null) {
      throw connectionException;
    }
    if (responses.isEmpty()) {
      throw new IllegalStateException("No mock responses enqueued");
    }

    MockResponse response = responses.remove(0);
    return new HttpResponse() {
      @Override
      public int statusCode() {
        return response.statusCode();
      }

      @Override
      public Map<String, List<String>> headers() {
        return response.headers();
      }

      @Override
      public InputStream bodyStream() {
        return new ByteArrayInputStream(response.body());
      }

      @Override
      public byte[] bodyBytes() {
        return response.body().clone();
      }

      @Override
      public byte[] bodyBytes(int maxBytes) {
        if (response.body().length > maxBytes) {
          throw new ShareFileNetworkException("Response exceeds max size");
        }
        return response.body().clone();
      }

      @Override
      public void close() {}
    };
  }

  private static byte[] readRequestBody(HttpRequest request) {
    Optional<InputStream> bodyStream = request.bodyStream();
    if (bodyStream.isEmpty()) {
      return new byte[0];
    }
    try (InputStream stream = bodyStream.get()) {
      return stream.readAllBytes();
    } catch (IOException e) {
      throw new ShareFileNetworkException("Failed to read mock request body", e);
    }
  }

  public record RecordedRequest(
      URI uri,
      String method,
      String body,
      byte[] bodyBytes,
      Map<String, String> headers,
      Duration timeout) {}

  private record MockResponse(int statusCode, byte[] body, Map<String, List<String>> headers) {}
}
