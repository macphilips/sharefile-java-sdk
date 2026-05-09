package io.github.indraftapp.sharefile.core.exception;

/**
 * Base exception for upload and download errors.
 */
public class ShareFileTransferException extends ShareFileException {

    public ShareFileTransferException(String message) {
        super(message);
    }

    public ShareFileTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
