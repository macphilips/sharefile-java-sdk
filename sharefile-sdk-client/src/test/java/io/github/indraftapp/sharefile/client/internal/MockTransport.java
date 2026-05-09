package io.github.indraftapp.sharefile.client.internal;

import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Test mock for {@link HttpTransport}. Records requests and returns enqueued responses. */
final class MockTransport implements HttpTransport {

  final List<RecordedRequest> requests = Collections.synchronizedList(new ArrayList<>());
  private final List<MockResponse> responses = Collections.synchronizedList(new ArrayList<>());
  private ShareFileNetworkException connectionException;

  void enqueueJsonResponse(int statusCode, String body) {
    enqueueJsonResponse(statusCode, body, Map.of());
  }

  void enqueueJsonResponse(int statusCode, String body, Map<String, List<String>> headers) {
    responses.add(new MockResponse(statusCode, body, headers));
  }

  void setConnectionException(ShareFileNetworkException exception) {
    this.connectionException = exception;
  }

  void clearConnectionException() {
    this.connectionException = null;
  }

  RecordedRequest getLastRequest() {
    return requests.get(requests.size() - 1);
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

    if (connectionException != null) {
      throw connectionException;
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
        return mockResp.headers;
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
          throw new ShareFileNetworkException("Response exceeds max size");
        }
        return respBytes;
      }

      @Override
      public void close() {}
    };
  }

  record RecordedRequest(URI uri, String method, String body, Map<String, String> headers) {}

  record MockResponse(int statusCode, String body, Map<String, List<String>> headers) {}
}
