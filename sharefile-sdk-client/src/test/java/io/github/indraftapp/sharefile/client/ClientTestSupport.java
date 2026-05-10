package io.github.indraftapp.sharefile.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.auth.InMemoryTokenStore;
import io.github.indraftapp.sharefile.client.auth.TokenManager;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.internal.ShareFileHttpClient;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import io.github.indraftapp.sharefile.core.jackson.ShareFileObjectMapper;
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

final class ClientTestSupport {

  static final String BASE_URL = "https://testco.sf-api.com/sf/v3";
  static final ObjectMapper MAPPER = ShareFileObjectMapper.create();

  private ClientTestSupport() {}

  static TestContext createContext(TestTransport transport) {
    return createContext(transport, RetryConfig.builder().maxRetries(0).build());
  }

  static TestContext createContext(TestTransport transport, RetryConfig retryConfig) {
    return createContext(transport, retryConfig, MetricsProvider.noop());
  }

  static TestContext createContext(
      TestTransport transport, RetryConfig retryConfig, MetricsProvider metricsProvider) {
    ShareFileConfig config = ShareFileConfig.builder().subdomain("testco").build();
    OAuthToken initialToken = new OAuthToken();
    initialToken.setAccessToken("test-bearer-token");
    initialToken.setRefreshToken("refresh-token");
    initialToken.setExpiresIn(3600L);
    initialToken.setExpiresAt(Instant.now().plusSeconds(3600));

    TokenManager tokenManager =
        new TokenManager(
            config,
            "test-client-id",
            "test-client-secret",
            () ->
                Credentials.builder()
                    .clientCredentials("test-client-id", "test-client-secret")
                    .passwordGrant("user", "pass")
                    .build(),
            transport,
            new InMemoryTokenStore(),
            MAPPER,
            metricsProvider,
            initialToken);

    ShareFileHttpClient httpClient =
        new ShareFileHttpClient(
            transport,
            tokenManager,
            MAPPER,
            retryConfig,
            metricsProvider,
            BASE_URL,
            Duration.ofSeconds(30));

    return new TestContext(
        new ItemsClient(httpClient),
        new AccessControlsClient(httpClient),
        new AsyncOperationsClient(httpClient),
        new SharesClient(httpClient),
        new UsersClient(httpClient),
        new GroupsClient(httpClient),
        new WebhookSubscriptionsClient(httpClient),
        new TransferClient(httpClient, transport, MAPPER, config, retryConfig, metricsProvider),
        new ResourceRequestExecutor(httpClient, "/Items"),
        transport,
        tokenManager);
  }

  record TestContext(
      ItemsClient itemsClient,
      AccessControlsClient accessControlsClient,
      AsyncOperationsClient asyncOperationsClient,
      SharesClient sharesClient,
      UsersClient usersClient,
      GroupsClient groupsClient,
      WebhookSubscriptionsClient webhookSubscriptionsClient,
      TransferClient transferClient,
      ResourceRequestExecutor executor,
      TestTransport transport,
      TokenManager tokenManager)
      implements AutoCloseable {

    @Override
    public void close() {
      tokenManager.close();
    }
  }

  static final class TestTransport implements HttpTransport {

    final List<RecordedRequest> requests = Collections.synchronizedList(new ArrayList<>());
    private final List<MockResponse> responses = Collections.synchronizedList(new ArrayList<>());
    private ShareFileNetworkException connectionException;

    void enqueueJsonResponse(int statusCode, String body) {
      enqueueJsonResponse(statusCode, body, Map.of());
    }

    void enqueueJsonResponse(int statusCode, String body, Map<String, List<String>> headers) {
      responses.add(new MockResponse(statusCode, body.getBytes(StandardCharsets.UTF_8), headers));
    }

    void enqueueResponse(int statusCode, byte[] body) {
      enqueueResponse(statusCode, body, Map.of());
    }

    void enqueueResponse(int statusCode, byte[] body, Map<String, List<String>> headers) {
      responses.add(new MockResponse(statusCode, body, headers));
    }

    RecordedRequest getLastRequest() {
      return requests.get(requests.size() - 1);
    }

    void setConnectionException(ShareFileNetworkException exception) {
      this.connectionException = exception;
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
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
        throw new IllegalStateException("No mock responses enqueued");
      }

      MockResponse mockResponse = responses.remove(0);
      byte[] responseBytes = mockResponse.body();

      return new HttpResponse() {
        private final InputStream stream = new ByteArrayInputStream(responseBytes);

        @Override
        public int statusCode() {
          return mockResponse.statusCode();
        }

        @Override
        public Map<String, List<String>> headers() {
          return mockResponse.headers();
        }

        @Override
        public InputStream bodyStream() {
          return stream;
        }

        @Override
        public byte[] bodyBytes() {
          return responseBytes;
        }

        @Override
        public byte[] bodyBytes(int maxBytes) {
          if (responseBytes.length > maxBytes) {
            throw new ShareFileNetworkException("Response exceeds max size");
          }
          return responseBytes;
        }

        @Override
        public void close() {}
      };
    }
  }

  record RecordedRequest(URI uri, String method, String body, Map<String, String> headers) {}

  record MockResponse(int statusCode, byte[] body, Map<String, List<String>> headers) {}
}
