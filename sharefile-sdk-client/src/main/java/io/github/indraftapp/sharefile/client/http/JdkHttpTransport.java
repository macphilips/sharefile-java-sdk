package io.github.indraftapp.sharefile.client.http;

import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default {@link HttpTransport} implementation using {@code java.net.http.HttpClient}.
 *
 * <p>Key design choices:
 * <ul>
 *   <li>Response bodies are always streamed via {@code BodyHandlers.ofInputStream()}
 *       — never buffered in full</li>
 *   <li>Upload bodies are streamed from the caller-provided {@link InputStream}
 *       while preserving a fixed {@code Content-Length} when the caller provides one</li>
 *   <li>Redirects are set to {@code NEVER} — the SDK controls redirect handling
 *       to prevent leaking authorization headers to storage-zone hosts</li>
 *   <li>{@code IOException} and {@code InterruptedException} are wrapped in
 *       {@link ShareFileNetworkException}</li>
 * </ul>
 */
public final class JdkHttpTransport implements HttpTransport {

    private static final int BODY_CHUNK_SIZE = 8192;
    private static final ScheduledExecutorService RESPONSE_TIMEOUT_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(new ResponseTimeoutThreadFactory());

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
        Objects.requireNonNull(request.timeout(), "request timeout must not be null");
        long deadlineNanos = System.nanoTime() + request.timeout().toNanos();

        try {
            var jdkRequest = toJdkRequest(request);
            var jdkResponse = httpClient.send(jdkRequest, BodyHandlers.ofInputStream());
            return new JdkHttpResponse(jdkResponse, deadlineNanos, request.timeout(), request.method(), request.uri());
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
                        return BodyPublishers.fromPublisher(
                                new InputStreamBodyPublisher(stream),
                                contentLength.getAsLong()
                        );
                    } else {
                        return BodyPublishers.fromPublisher(new InputStreamBodyPublisher(stream));
                    }
                })
                .orElse(BodyPublishers.noBody());

        builder.method(request.method(), bodyPublisher);
        return builder.build();
    }

    // ── Response wrapper ─────────────────────────────────────────────────

    private static final class JdkHttpResponse implements HttpResponse {

        private final int statusCode;
        private final Map<String, List<String>> headers;
        private final TimeoutAwareInputStream bodyStream;
        private final String requestDescription;

        JdkHttpResponse(java.net.http.HttpResponse<InputStream> response, long deadlineNanos,
                        java.time.Duration timeout, String method, URI uri) {
            this.requestDescription = "%s %s".formatted(method, uri);
            this.statusCode = response.statusCode();
            var normalized = new LinkedHashMap<String, List<String>>();
            response.headers().map().forEach((name, values) ->
                    normalized.put(name.toLowerCase(Locale.ROOT), List.copyOf(values)));
            this.headers = Map.copyOf(normalized);
            this.bodyStream = new TimeoutAwareInputStream(response.body(), deadlineNanos, timeout, requestDescription);
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public Map<String, List<String>> headers() {
            return headers;
        }

        @Override
        public InputStream bodyStream() {
            return bodyStream;
        }

        @Override
        public byte[] bodyBytes() throws ShareFileNetworkException {
            try (var stream = bodyStream) {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new ShareFileNetworkException(
                        "Failed to read response body for %s: %s".formatted(requestDescription, e.getMessage()), e);
            }
        }

        @Override
        public byte[] bodyBytes(int maxBytes) throws ShareFileNetworkException {
            if (maxBytes <= 0) {
                throw new IllegalArgumentException("maxBytes must be positive: " + maxBytes);
            }
            try (var stream = bodyStream) {
                var buffer = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
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
                throw new ShareFileNetworkException(
                        "Failed to read response body for %s: %s".formatted(requestDescription, e.getMessage()), e);
            }
        }

        @Override
        public void close() {
            bodyStream.closeQuietly();
        }
    }

    private static final class TimeoutAwareInputStream extends InputStream {

        private final InputStream delegate;
        private final String timeoutMessage;
        private final AtomicBoolean timedOut = new AtomicBoolean(false);
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final ScheduledFuture<?> timeoutFuture;

        private TimeoutAwareInputStream(InputStream delegate, long deadlineNanos, java.time.Duration timeout, String requestDescription) {
            this.delegate = delegate;
            this.timeoutMessage = "%s timed out while reading the response body after %s"
                    .formatted(requestDescription, timeout);
            long delayNanos = Math.max(0L, deadlineNanos - System.nanoTime());
            this.timeoutFuture = RESPONSE_TIMEOUT_EXECUTOR.schedule(this::timeoutClose, delayNanos, TimeUnit.NANOSECONDS);
        }

        @Override
        public int read() throws IOException {
            ensureNotTimedOut();
            try {
                int read = delegate.read();
                if (read == -1) {
                    cancelTimeout();
                }
                return read;
            } catch (IOException e) {
                throw wrapTimeout(e);
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureNotTimedOut();
            try {
                int read = delegate.read(b, off, len);
                if (read == -1) {
                    cancelTimeout();
                }
                return read;
            } catch (IOException e) {
                throw wrapTimeout(e);
            }
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                cancelTimeout();
                delegate.close();
            }
        }

        private void timeoutClose() {
            if (closed.get()) {
                return;
            }
            timedOut.set(true);
            try {
                delegate.close();
            } catch (IOException ignored) {
                // Best-effort close when the deadline expires.
            }
        }

        private void ensureNotTimedOut() throws IOException {
            if (timedOut.get()) {
                throw newTimeoutException();
            }
        }

        private IOException wrapTimeout(IOException cause) {
            if (!timedOut.get()) {
                return cause;
            }
            var timeoutException = newTimeoutException();
            timeoutException.initCause(cause);
            return timeoutException;
        }

        private InterruptedIOException newTimeoutException() {
            return new InterruptedIOException(timeoutMessage);
        }

        private void cancelTimeout() {
            timeoutFuture.cancel(false);
        }

        private void closeQuietly() {
            try {
                close();
            } catch (IOException ignored) {
                // Best-effort close.
            }
        }
    }

    private static final class InputStreamBodyPublisher implements Flow.Publisher<ByteBuffer> {

        private final InputStream inputStream;
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        private InputStreamBodyPublisher(InputStream inputStream) {
            this.inputStream = Objects.requireNonNull(inputStream, "inputStream must not be null");
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            Objects.requireNonNull(subscriber, "subscriber must not be null");
            if (!subscribed.compareAndSet(false, true)) {
                subscriber.onError(new IllegalStateException("Request body publisher only supports one subscriber"));
                return;
            }
            subscriber.onSubscribe(new InputStreamSubscription(subscriber, inputStream));
        }
    }

    private static final class InputStreamSubscription implements Flow.Subscription {

        private final Flow.Subscriber<? super ByteBuffer> subscriber;
        private final InputStream inputStream;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private boolean draining;
        private boolean completed;

        private InputStreamSubscription(Flow.Subscriber<? super ByteBuffer> subscriber, InputStream inputStream) {
            this.subscriber = subscriber;
            this.inputStream = inputStream;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                cancel();
                subscriber.onError(new IllegalArgumentException("Demand must be positive"));
                return;
            }

            addDemand(n);
            drain();
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                closeInputStream();
            }
        }

        private void addDemand(long n) {
            demand.accumulateAndGet(n, (current, increment) -> {
                long sum = current + increment;
                return sum < 0 ? Long.MAX_VALUE : sum;
            });
        }

        private void drain() {
            synchronized (this) {
                if (draining || completed || cancelled.get()) {
                    return;
                }
                draining = true;
            }

            try {
                while (!cancelled.get()) {
                    long currentDemand = demand.get();
                    if (currentDemand <= 0) {
                        return;
                    }

                    byte[] chunk = new byte[BODY_CHUNK_SIZE];
                    int bytesRead = inputStream.read(chunk);
                    if (bytesRead == -1) {
                        completed = true;
                        closeInputStream();
                        subscriber.onComplete();
                        return;
                    }

                    subscriber.onNext(ByteBuffer.wrap(chunk, 0, bytesRead));
                    demand.decrementAndGet();
                }
            } catch (IOException e) {
                completed = true;
                closeInputStream();
                subscriber.onError(e);
            } finally {
                synchronized (this) {
                    draining = false;
                }
                if (demand.get() > 0 && !completed && !cancelled.get()) {
                    drain();
                }
            }
        }

        private void closeInputStream() {
            try {
                inputStream.close();
            } catch (IOException ignored) {
                // Best-effort cleanup when the publisher is cancelled or completes.
            }
        }
    }

    private static final class ResponseTimeoutThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "sharefile-http-response-timeouts");
            thread.setDaemon(true);
            return thread;
        }
    }
}
