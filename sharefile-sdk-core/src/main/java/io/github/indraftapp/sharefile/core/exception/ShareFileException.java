package io.github.indraftapp.sharefile.core.exception;

/** Base exception for all ShareFile SDK errors. */
public class ShareFileException extends RuntimeException {

  public ShareFileException(String message) {
    super(message);
  }

  public ShareFileException(String message, Throwable cause) {
    super(message, cause);
  }
}
