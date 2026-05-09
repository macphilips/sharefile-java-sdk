package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when a file download fails.
 */
public class ShareFileDownloadException extends ShareFileTransferException {

    public ShareFileDownloadException(String message) {
        super(message);
    }

    public ShareFileDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
