package io.github.indraftapp.sharefile.core.exception;

/** Thrown when the ShareFile API returns 403 Forbidden. */
public class ShareFileForbiddenException extends ShareFileApiException {

  public ShareFileForbiddenException(
      String errorCode,
      String errorMessage,
      String requestId,
      String requestMethod,
      String requestUri) {
    super(403, errorCode, errorMessage, requestId, requestMethod, requestUri);
  }
}
