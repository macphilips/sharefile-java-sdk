package io.github.indraftapp.sharefile.core.exception;

/**
 * Exception thrown when the ShareFile API returns an error response.
 */
public class ShareFileApiException extends ShareFileException {

    private final int httpStatus;
    private final String errorCode;
    private final String errorMessage;
    private final String requestId;
    private final String requestMethod;
    private final String requestUri;

    public ShareFileApiException(int httpStatus, String errorCode, String errorMessage,
                                 String requestId, String requestMethod, String requestUri) {
        super("ShareFile API error %d: %s (requestId=%s, %s %s)"
                .formatted(httpStatus, errorMessage, requestId, requestMethod, requestUri));
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
        this.requestMethod = requestMethod;
        this.requestUri = requestUri;
    }

    public ShareFileApiException(int httpStatus, String errorCode, String errorMessage,
                                 String requestId, String requestMethod, String requestUri,
                                 Throwable cause) {
        super("ShareFile API error %d: %s (requestId=%s, %s %s)"
                .formatted(httpStatus, errorMessage, requestId, requestMethod, requestUri), cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
        this.requestMethod = requestMethod;
        this.requestUri = requestUri;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestUri() {
        return requestUri;
    }
}
