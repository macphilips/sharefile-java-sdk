package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when a resolved download URL has expired.
 */
public class ShareFileDownloadUrlExpiredException extends ShareFileDownloadException {

    public ShareFileDownloadUrlExpiredException(String message) {
        super(message);
    }
}
