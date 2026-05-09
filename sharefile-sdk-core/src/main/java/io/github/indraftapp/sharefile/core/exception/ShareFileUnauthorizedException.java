package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when the ShareFile API returns 401 Unauthorized.
 */
public class ShareFileUnauthorizedException extends ShareFileApiException {

    public ShareFileUnauthorizedException(String errorCode, String errorMessage,
                                          String requestId, String requestMethod, String requestUri) {
        super(401, errorCode, errorMessage, requestId, requestMethod, requestUri);
    }
}
