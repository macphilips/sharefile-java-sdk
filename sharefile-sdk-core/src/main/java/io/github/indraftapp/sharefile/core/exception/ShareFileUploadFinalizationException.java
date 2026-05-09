package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when upload finalization (phase 3) fails.
 */
public class ShareFileUploadFinalizationException extends ShareFileUploadException {

    public ShareFileUploadFinalizationException(String message, long bytesTransferred) {
        super(message, false, -1, bytesTransferred);
    }

    public ShareFileUploadFinalizationException(String message, Throwable cause, long bytesTransferred) {
        super(message, cause, false, -1, bytesTransferred);
    }
}
