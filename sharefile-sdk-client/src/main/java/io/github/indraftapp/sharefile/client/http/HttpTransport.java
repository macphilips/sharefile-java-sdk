package io.github.indraftapp.sharefile.client.http;

import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * SPI for HTTP communication with the ShareFile API.
 *
 * <p>Implementations must support streaming request and response bodies.
 * File transfers can be multi-GB — the transport must never require buffering
 * entire upload/download bodies as byte arrays.
 *
 * <p>The default implementation is {@link JdkHttpTransport}, which uses
 * {@code java.net.http.HttpClient}.
 *
 * @see JdkHttpTransport
 */
public interface HttpTransport {

    /**
     * Executes an HTTP request and returns the response.
     *
     * @param request the request to execute
     * @return the HTTP response with a streaming body
     * @throws ShareFileNetworkException on connection failure, timeout, or I/O error
     */
    HttpResponse execute(HttpRequest request) throws ShareFileNetworkException;

    /**
     * An HTTP request with optional streaming body support.
     */
    interface HttpRequest {

        /**
         * The HTTP method (GET, POST, PUT, PATCH, DELETE).
         */
        String method();

        /**
         * The target URI.
         */
        URI uri();

        /**
         * Request headers. Implementations should treat this map as read-only.
         */
        Map<String, String> headers();

        /**
         * The request body as a stream, for uploads and JSON payloads.
         *
         * @return the body stream, or empty for bodyless requests (GET, DELETE)
         */
        Optional<InputStream> bodyStream();

        /**
         * The content length of the request body.
         *
         * <p>Returns empty if the body length is unknown (chunked transfer encoding).
         * Returns empty if there is no body.
         *
         * @return the content length, or empty for chunked/no body
         */
        OptionalLong contentLength();

        /**
         * The request timeout. This is the total time allowed for the request
         * to complete, including reading the response.
         */
        Duration timeout();
    }

    /**
     * An HTTP response with a streaming body.
     *
     * <p>Callers must close the {@link #bodyStream()} when done to release
     * the underlying connection.
     */
    interface HttpResponse extends AutoCloseable {

        /**
         * The HTTP status code.
         */
        int statusCode();

        /**
         * Response headers. Header names are case-insensitive per HTTP spec;
         * implementations should normalize to lowercase.
         */
        Map<String, List<String>> headers();

        /**
         * The response body as a stream. Always returns a valid stream,
         * even for empty bodies (returns an empty stream).
         *
         * <p>For file downloads, consume this stream directly to avoid
         * buffering the entire response in memory.
         */
        InputStream bodyStream();

        /**
         * Convenience method that reads the entire response body into a byte array.
         *
         * <p>Use this only for JSON API responses (typically &lt; 1 MB).
         * For file downloads, use {@link #bodyStream()} instead.
         *
         * @return the full response body as bytes
         * @throws ShareFileNetworkException if reading fails
         */
        byte[] bodyBytes() throws ShareFileNetworkException;

        /**
         * Reads the response body into a byte array, bounded by the given limit.
         *
         * @param maxBytes maximum number of bytes to read
         * @return the response body as bytes (up to maxBytes)
         * @throws ShareFileNetworkException if reading fails or the body exceeds maxBytes
         */
        byte[] bodyBytes(int maxBytes) throws ShareFileNetworkException;

        /**
         * Closes the response body stream and releases the underlying connection.
         */
        @Override
        void close();
    }
}
