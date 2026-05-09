package io.github.indraftapp.sharefile.core.exception;

/** Thrown when a transfer is cancelled cooperatively. */
public class ShareFileTransferCancelledException extends ShareFileTransferException {

  public ShareFileTransferCancelledException(String message) {
    super(message);
  }
}
