package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown on connection timeout, DNS resolution failure, or TLS errors.
 */
public class ShareFileNetworkException extends ShareFileException {

    public ShareFileNetworkException(String message) {
        super(message);
    }

    public ShareFileNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
