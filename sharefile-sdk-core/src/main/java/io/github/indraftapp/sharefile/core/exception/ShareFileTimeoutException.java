package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when an async operation polling exceeds the specified timeout.
 */
public class ShareFileTimeoutException extends ShareFileException {

    public ShareFileTimeoutException(String message) {
        super(message);
    }

    public ShareFileTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
