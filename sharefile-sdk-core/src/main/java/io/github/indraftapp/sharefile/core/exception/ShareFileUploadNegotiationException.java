package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when upload negotiation (phase 1) fails.
 */
public class ShareFileUploadNegotiationException extends ShareFileUploadException {

    public ShareFileUploadNegotiationException(String message) {
        super(message, false, -1, 0);
    }

    public ShareFileUploadNegotiationException(String message, Throwable cause) {
        super(message, cause, false, -1, 0);
    }
}
