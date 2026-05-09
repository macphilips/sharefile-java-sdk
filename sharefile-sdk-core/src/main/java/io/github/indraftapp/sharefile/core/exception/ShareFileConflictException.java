package io.github.indraftapp.sharefile.core.exception;

/** Thrown when the ShareFile API returns 409 Conflict. */
public class ShareFileConflictException extends ShareFileApiException {

  public ShareFileConflictException(
      String errorCode,
      String errorMessage,
      String requestId,
      String requestMethod,
      String requestUri) {
    super(409, errorCode, errorMessage, requestId, requestMethod, requestUri);
  }
}
