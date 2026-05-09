package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when the ShareFile API returns a 5xx server error.
 */
public class ShareFileServerException extends ShareFileApiException {

    public ShareFileServerException(int httpStatus, String errorCode, String errorMessage,
                                    String requestId, String requestMethod, String requestUri) {
        super(httpStatus, errorCode, errorMessage, requestId, requestMethod, requestUri);
    }

    public ShareFileServerException(int httpStatus, String errorCode, String errorMessage,
                                    String requestId, String requestMethod, String requestUri,
                                    Throwable cause) {
        super(httpStatus, errorCode, errorMessage, requestId, requestMethod, requestUri, cause);
    }
}
