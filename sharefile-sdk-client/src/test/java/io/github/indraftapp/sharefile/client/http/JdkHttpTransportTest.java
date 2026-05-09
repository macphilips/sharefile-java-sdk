package io.github.indraftapp.sharefile.client.http;

import com.sun.net.httpserver.HttpServer;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-06: Tests for {@link JdkHttpTransport} using a local HTTP server.
 */
class JdkHttpTransportTest {

    private HttpServer server;
    private int port;
    private JdkHttpTransport transport;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.setExecutor(null);

        var config = ShareFileConfig.builder()
                .subdomain("test")
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        transport = new JdkHttpTransport(config);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private URI localUri(String path) {
        return URI.create("http://localhost:%d%s".formatted(port, path));
    }

    // ── GET with status and streaming body ───────────────────────────────

    @Test
    void get_returnsStatusAndStreamingBody() throws Exception {
        server.createContext("/test", exchange -> {
            byte[] body = "{\"Id\":\"abc\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        var request = HttpRequests.noBody("GET", localUri("/test"), Duration.ofSeconds(5));

        try (var response = transport.execute(request)) {
            assertThat(response.statusCode()).isEqualTo(200);

            // Read via bodyStream()
            try (var stream = response.bodyStream()) {
                String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(content).isEqualTo("{\"Id\":\"abc\"}");
            }
        }
    }

    // ── JSON response via bodyBytes() ────────────────────────────────────

    @Test
    void get_bodyBytes_returnsFullArray() throws Exception {
        String json = "{\"Name\":\"Reports\",\"FileCount\":42}";
        server.createContext("/json", exchange -> {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        var request = HttpRequests.noBody("GET", localUri("/json"), Duration.ofSeconds(5));

        try (var response = transport.execute(request)) {
            byte[] bytes = response.bodyBytes();
            assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(json);
        }
    }

    // ── bodyBytes(maxBytes) bounded read ─────────────────────────────────

    @Test
    void get_bodyBytes_bounded_succeeds() throws Exception {
        server.createContext("/small", exchange -> {
            byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        var request = HttpRequests.noBody("GET", localUri("/small"), Duration.ofSeconds(5));

        try (var response = transport.execute(request)) {
            byte[] bytes = response.bodyBytes(1024);
            assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("hello");
        }
    }

    @Test
    void get_bodyBytes_bounded_throwsWhenExceeded() throws Exception {
        server.createContext("/large", exchange -> {
            byte[] body = "this is more than 5 bytes".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        var request = HttpRequests.noBody("GET", localUri("/large"), Duration.ofSeconds(5));

        try (var response = transport.execute(request)) {
            assertThatThrownBy(() -> response.bodyBytes(5))
                    .isInstanceOf(ShareFileNetworkException.class)
                    .hasMessageContaining("exceeds maximum size");
        }
    }

    // ── Large response via bodyStream() incremental consumption ──────────

    @Test
    void get_largeResponse_canBeConsumedIncrementally() throws Exception {
        int totalSize = 64 * 1024; // 64 KB
        server.createContext("/stream", exchange -> {
            exchange.sendResponseHeaders(200, totalSize);
            try (var os = exchange.getResponseBody()) {
                byte[] chunk = new byte[1024];
                java.util.Arrays.fill(chunk, (byte) 'X');
                for (int i = 0; i < totalSize / chunk.length; i++) {
                    os.write(chunk);
                }
            }
        });
        server.start();

        var request = HttpRequests.noBody("GET", localUri("/stream"), Duration.ofSeconds(10));

        try (var response = transport.execute(request)) {
            // Read incrementally in chunks — not buffering the whole thing
            try (var stream = response.bodyStream()) {
                byte[] buf = new byte[4096];
                long total = 0;
                int read;
                while ((read = stream.read(buf)) != -1) {
                    total += read;
                }
                assertThat(total).isEqualTo(totalSize);
            }
        }
    }

    // ── POST with JSON body ──────────────────────────────────────────────

    @Test
    void post_jsonBody_sentCorrectly() throws Exception {
        String sentJson = "{\"Name\":\"NewFolder\"}";

        server.createContext("/items", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            assertThat(contentType).isEqualTo("application/json");

            String received;
            try (var is = exchange.getRequestBody()) {
                received = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertThat(received).isEqualTo(sentJson);

            byte[] resp = "{\"Id\":\"new-1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, resp.length);
            try (var os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();

        var request = HttpRequests.json(
                "POST", localUri("/items"),
                sentJson.getBytes(StandardCharsets.UTF_8),
                Duration.ofSeconds(5)
        );

        try (var response = transport.execute(request)) {
            assertThat(response.statusCode()).isEqualTo(201);
        }
    }

    // ── Upload with streaming InputStream body ───────────────────────────

    @Test
    void post_streamingBody_sentWithoutBuffering() throws Exception {
        byte[] uploadData = "file-content-bytes-here".getBytes(StandardCharsets.UTF_8);

        server.createContext("/upload", exchange -> {
            String received;
            try (var is = exchange.getRequestBody()) {
                received = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertThat(received).isEqualTo("file-content-bytes-here");

            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        var request = HttpRequests.streaming(
                "POST", localUri("/upload"),
                new ByteArrayInputStream(uploadData), uploadData.length,
                Duration.ofSeconds(5)
        );

        try (var response = transport.execute(request)) {
            assertThat(response.statusCode()).isEqualTo(200);
        }
    }

    // ── Connection timeout produces ShareFileNetworkException ────────────

    @Test
    void connectionTimeout_throwsShareFileNetworkException() {
        // Connect to a non-routable address to trigger timeout
        var config = ShareFileConfig.builder()
                .subdomain("test")
                .connectTimeout(Duration.ofMillis(100))
                .build();
        var timeoutTransport = new JdkHttpTransport(config);

        var request = HttpRequests.noBody(
                "GET",
                URI.create("http://198.51.100.1:1/timeout"), // RFC 5737 test address
                Duration.ofMillis(500)
        );

        assertThatThrownBy(() -> timeoutTransport.execute(request))
                .isInstanceOf(ShareFileNetworkException.class)
                .hasMessageContaining("GET");
    }

    // ── Redirect not followed ────────────────────────────────────────────

    @Test
    void redirect_notFollowed() throws Exception {
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:%d/target".formatted(port));
            exchange.sendResponseHeaders(302, -1);
        });
        server.createContext("/target", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        var request = HttpRequests.noBody("GET", localUri("/redirect"), Duration.ofSeconds(5));

        try (var response = transport.execute(request)) {
            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(response.headers()).containsKey("location");
        }
    }

    // ── Response headers are case-insensitive ────────────────────────────

    @Test
    void responseHeaders_caseInsensitive() throws Exception {
        server.createContext("/headers", exchange -> {
            exchange.getResponseHeaders().add("X-Custom-Header", "test-value");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        var request = HttpRequests.noBody("GET", localUri("/headers"), Duration.ofSeconds(5));

        try (var response = transport.execute(request)) {
            // Should find header regardless of case
            assertThat(response.headers().get("x-custom-header")).isNotNull();
        }
    }

    // ── DELETE without body ──────────────────────────────────────────────

    @Test
    void delete_noBodY_succeeds() throws Exception {
        server.createContext("/items/abc", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("DELETE");
            exchange.sendResponseHeaders(204, -1);
        });
        server.start();

        var request = HttpRequests.noBody("DELETE", localUri("/items/abc"), Duration.ofSeconds(5));

        try (var response = transport.execute(request)) {
            assertThat(response.statusCode()).isEqualTo(204);
        }
    }

    // ── Form-encoded request ─────────────────────────────────────────────

    @Test
    void post_formEncoded_sentCorrectly() throws Exception {
        String formBody = "grant_type=password&username=user&password=pass";

        server.createContext("/oauth/token", exchange -> {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            assertThat(contentType).isEqualTo("application/x-www-form-urlencoded");

            String received;
            try (var is = exchange.getRequestBody()) {
                received = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertThat(received).isEqualTo(formBody);

            byte[] resp = "{\"access_token\":\"tok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (var os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();

        var request = HttpRequests.formEncoded(
                "POST", localUri("/oauth/token"),
                formBody.getBytes(StandardCharsets.UTF_8),
                Duration.ofSeconds(5)
        );

        try (var response = transport.execute(request)) {
            assertThat(response.statusCode()).isEqualTo(200);
            String body = new String(response.bodyBytes(), StandardCharsets.UTF_8);
            assertThat(body).contains("access_token");
        }
    }
}
