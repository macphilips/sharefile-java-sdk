package io.github.indraftapp.sharefile.core.exception;

/** Thrown when the ShareFile API returns 404 Not Found. */
public class ShareFileNotFoundException extends ShareFileApiException {

  public ShareFileNotFoundException(
      String errorCode,
      String errorMessage,
      String requestId,
      String requestMethod,
      String requestUri) {
    super(404, errorCode, errorMessage, requestId, requestMethod, requestUri);
  }
}
