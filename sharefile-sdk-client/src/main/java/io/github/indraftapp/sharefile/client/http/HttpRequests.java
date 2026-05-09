package io.github.indraftapp.sharefile.client.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Package-private factory for creating {@link HttpTransport.HttpRequest} instances.
 */
final class HttpRequests {

    private HttpRequests() {}

    /**
     * Creates a JSON API request with a byte-array body.
     *
     * @param method  HTTP method (POST, PUT, PATCH)
     * @param uri     target URI
     * @param body    JSON body as bytes
     * @param timeout request timeout
     * @return a new HTTP request
     */
    static HttpTransport.HttpRequest json(String method, URI uri, byte[] body, Duration timeout) {
        Objects.requireNonNull(body, "body must not be null");
        var headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return new DefaultHttpRequest(
                method, uri, Collections.unmodifiableMap(headers),
                new ByteArrayInputStream(body), body.length, timeout
        );
    }

    /**
     * Creates a streaming upload request with an {@link InputStream} body.
     *
     * @param method      HTTP method (POST, PUT)
     * @param uri         target URI
     * @param inputStream the body stream (not closed by this method)
     * @param size        the content length, or -1 if unknown (chunked)
     * @param timeout     request timeout
     * @return a new HTTP request
     */
    static HttpTransport.HttpRequest streaming(String method, URI uri, InputStream inputStream,
                                               long size, Duration timeout) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        var headers = new LinkedHashMap<String, String>();
        headers.put("Accept", "application/json");
        return new DefaultHttpRequest(
                method, uri, Collections.unmodifiableMap(headers),
                inputStream, size, timeout
        );
    }

    /**
     * Creates a bodyless request (GET, DELETE, HEAD).
     *
     * @param method  HTTP method
     * @param uri     target URI
     * @param timeout request timeout
     * @return a new HTTP request
     */
    static HttpTransport.HttpRequest noBody(String method, URI uri, Duration timeout) {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Accept", "application/json");
        return new DefaultHttpRequest(
                method, uri, Collections.unmodifiableMap(headers),
                null, -1, timeout
        );
    }

    /**
     * Creates a form-encoded request (used for OAuth token endpoint).
     *
     * @param method  HTTP method (typically POST)
     * @param uri     target URI
     * @param body    form-encoded body as bytes
     * @param timeout request timeout
     * @return a new HTTP request
     */
    static HttpTransport.HttpRequest formEncoded(String method, URI uri, byte[] body, Duration timeout) {
        Objects.requireNonNull(body, "body must not be null");
        var headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "application/json");
        return new DefaultHttpRequest(
                method, uri, Collections.unmodifiableMap(headers),
                new ByteArrayInputStream(body), body.length, timeout
        );
    }

    // ── Default implementation ───────────────────────────────────────────

    private record DefaultHttpRequest(
            String method,
            URI uri,
            Map<String, String> headers,
            InputStream body,
            long size,
            Duration timeout
    ) implements HttpTransport.HttpRequest {

        @Override
        public Optional<InputStream> bodyStream() {
            return Optional.ofNullable(body);
        }

        @Override
        public OptionalLong contentLength() {
            if (body == null || size < 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(size);
        }
    }
}
