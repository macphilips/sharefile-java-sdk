package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when OAuth2 token acquisition or refresh fails.
 */
public class ShareFileAuthenticationException extends ShareFileException {

    public ShareFileAuthenticationException(String message) {
        super(message);
    }

    public ShareFileAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
