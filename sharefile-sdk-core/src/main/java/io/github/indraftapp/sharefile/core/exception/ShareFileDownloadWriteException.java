package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when writing downloaded bytes to disk fails.
 */
public class ShareFileDownloadWriteException extends ShareFileDownloadException {

    public ShareFileDownloadWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
