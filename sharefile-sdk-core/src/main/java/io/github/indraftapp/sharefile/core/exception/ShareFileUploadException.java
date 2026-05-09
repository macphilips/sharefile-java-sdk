package io.github.indraftapp.sharefile.core.exception;

import lombok.Getter;

/** Thrown when a file upload fails. */
@Getter
public class ShareFileUploadException extends ShareFileTransferException {

  private final boolean resumable;
  private final int lastSuccessfulChunkIndex;
  private final long bytesTransferred;

  public ShareFileUploadException(
      String message, boolean resumable, int lastSuccessfulChunkIndex, long bytesTransferred) {
    super(message);
    this.resumable = resumable;
    this.lastSuccessfulChunkIndex = lastSuccessfulChunkIndex;
    this.bytesTransferred = bytesTransferred;
  }

  public ShareFileUploadException(
      String message,
      Throwable cause,
      boolean resumable,
      int lastSuccessfulChunkIndex,
      long bytesTransferred) {
    super(message, cause);
    this.resumable = resumable;
    this.lastSuccessfulChunkIndex = lastSuccessfulChunkIndex;
    this.bytesTransferred = bytesTransferred;
  }
}
