package io.github.indraftapp.sharefile.client.http;

import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Default {@link HttpTransport} implementation using {@code java.net.http.HttpClient}.
 *
 * <p>Key design choices:
 * <ul>
 *   <li>Response bodies are always streamed via {@code BodyHandlers.ofInputStream()}
 *       — never buffered in full</li>
 *   <li>Upload bodies are streamed from the caller-provided {@link InputStream}
 *       via {@code BodyPublishers.ofInputStream()}</li>
 *   <li>Redirects are set to {@code NEVER} — the SDK controls redirect handling
 *       to prevent leaking authorization headers to storage-zone hosts</li>
 *   <li>{@code IOException} and {@code InterruptedException} are wrapped in
 *       {@link ShareFileNetworkException}</li>
 * </ul>
 */
public final class JdkHttpTransport implements HttpTransport {

    private final HttpClient httpClient;

    /**
     * Creates a transport with settings from the given configuration.
     *
     * @param config the ShareFile configuration
     */
    public JdkHttpTransport(ShareFileConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Creates a transport with a pre-built {@link HttpClient}.
     *
     * <p>This constructor is primarily for testing. The caller is responsible
     * for configuring the client appropriately (redirect policy, timeouts, etc.).
     *
     * @param httpClient the HTTP client to use
     */
    JdkHttpTransport(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws ShareFileNetworkException {
        Objects.requireNonNull(request, "request must not be null");

        try {
            var jdkRequest = toJdkRequest(request);
            var jdkResponse = httpClient.send(jdkRequest, BodyHandlers.ofInputStream());
            return new JdkHttpResponse(jdkResponse);
        } catch (IOException e) {
            throw new ShareFileNetworkException(
                    "%s %s failed: %s".formatted(request.method(), request.uri(), e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShareFileNetworkException(
                    "%s %s interrupted".formatted(request.method(), request.uri()), e);
        }
    }

    private java.net.http.HttpRequest toJdkRequest(HttpRequest request) {
        var builder = java.net.http.HttpRequest.newBuilder()
                .uri(request.uri())
                .timeout(request.timeout());

        // Set headers
        request.headers().forEach(builder::header);

        // Set method and body
        BodyPublisher bodyPublisher = request.bodyStream()
                .map(stream -> {
                    var contentLength = request.contentLength();
                    if (contentLength.isPresent()) {
                        return BodyPublishers.ofInputStream(() -> stream);
                    } else {
                        return BodyPublishers.ofInputStream(() -> stream);
                    }
                })
                .orElse(BodyPublishers.noBody());

        builder.method(request.method(), bodyPublisher);
        return builder.build();
    }

    // ── Response wrapper ─────────────────────────────────────────────────

    private static final class JdkHttpResponse implements HttpResponse {

        private final java.net.http.HttpResponse<InputStream> response;
        private final Map<String, List<String>> headers;

        JdkHttpResponse(java.net.http.HttpResponse<InputStream> response) {
            this.response = response;
            // Normalize header names to lowercase for consistent lookup
            var normalized = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
            response.headers().map().forEach(normalized::put);
            this.headers = Map.copyOf(normalized);
        }

        @Override
        public int statusCode() {
            return response.statusCode();
        }

        @Override
        public Map<String, List<String>> headers() {
            return headers;
        }

        @Override
        public InputStream bodyStream() {
            return response.body();
        }

        @Override
        public byte[] bodyBytes() throws ShareFileNetworkException {
            try (var stream = response.body()) {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new ShareFileNetworkException("Failed to read response body", e);
            }
        }

        @Override
        public byte[] bodyBytes(int maxBytes) throws ShareFileNetworkException {
            if (maxBytes <= 0) {
                throw new IllegalArgumentException("maxBytes must be positive: " + maxBytes);
            }
            try (var stream = response.body()) {
                var buffer = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
                byte[] chunk = new byte[8192];
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
                response.body().close();
            } catch (IOException ignored) {
                // Best-effort close
            }
        }
    }
}
