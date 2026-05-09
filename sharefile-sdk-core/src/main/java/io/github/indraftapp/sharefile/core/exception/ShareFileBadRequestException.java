package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when the ShareFile API returns 400 Bad Request.
 */
public class ShareFileBadRequestException extends ShareFileApiException {

    public ShareFileBadRequestException(String errorCode, String errorMessage,
                                        String requestId, String requestMethod, String requestUri) {
        super(400, errorCode, errorMessage, requestId, requestMethod, requestUri);
    }
}
