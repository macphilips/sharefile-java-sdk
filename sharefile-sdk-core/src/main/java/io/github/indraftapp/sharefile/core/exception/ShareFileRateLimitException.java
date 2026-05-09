package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when the ShareFile API returns 429 Too Many Requests.
 */
public class ShareFileRateLimitException extends ShareFileApiException {

    private final long retryAfterSeconds;

    public ShareFileRateLimitException(String errorCode, String errorMessage,
                                       String requestId, String requestMethod, String requestUri,
                                       long retryAfterSeconds) {
        super(429, errorCode, errorMessage, requestId, requestMethod, requestUri);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
