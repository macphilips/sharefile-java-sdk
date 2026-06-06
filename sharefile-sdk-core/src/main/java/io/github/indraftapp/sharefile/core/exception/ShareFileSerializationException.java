package io.github.indraftapp.sharefile.core.exception;

/** Thrown when JSON serialization or deserialization fails. */
public class ShareFileSerializationException extends ShareFileException {

  public ShareFileSerializationException(String message) {
    super(message);
  }

  public ShareFileSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
