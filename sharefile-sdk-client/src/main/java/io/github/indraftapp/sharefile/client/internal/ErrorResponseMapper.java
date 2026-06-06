package io.github.indraftapp.sharefile.client.internal;

import io.github.indraftapp.sharefile.core.exception.ShareFileApiException;
import io.github.indraftapp.sharefile.core.exception.ShareFileBadRequestException;
import io.github.indraftapp.sharefile.core.exception.ShareFileConflictException;
import io.github.indraftapp.sharefile.core.exception.ShareFileForbiddenException;
import io.github.indraftapp.sharefile.core.exception.ShareFileNotFoundException;
import io.github.indraftapp.sharefile.core.exception.ShareFileRateLimitException;
import io.github.indraftapp.sharefile.core.exception.ShareFileServerException;
import io.github.indraftapp.sharefile.core.exception.ShareFileUnauthorizedException;

/**
 * Maps HTTP status codes to the appropriate {@link ShareFileApiException} subclass.
 *
 * <p>Package-private — not part of the public API.
 */
final class ErrorResponseMapper {

  private ErrorResponseMapper() {}

  /**
   * Creates the appropriate exception for the given status code.
   *
   * @param status HTTP status code
   * @param errorCode error code from response body
   * @param errorMessage error message from response body
   * @param requestId X-Request-Id header value
   * @param requestMethod HTTP method
   * @param requestUri request URI
   * @param retryAfterSeconds Retry-After value (only meaningful for 429)
   * @return the mapped exception
   */
  static ShareFileApiException create(
      int status,
      String errorCode,
      String errorMessage,
      String requestId,
      String requestMethod,
      String requestUri,
      long retryAfterSeconds) {
    return switch (status) {
      case 400 ->
          new ShareFileBadRequestException(
              errorCode, errorMessage, requestId, requestMethod, requestUri);
      case 401 ->
          new ShareFileUnauthorizedException(
              errorCode, errorMessage, requestId, requestMethod, requestUri);
      case 403 ->
          new ShareFileForbiddenException(
              errorCode, errorMessage, requestId, requestMethod, requestUri);
      case 404 ->
          new ShareFileNotFoundException(
              errorCode, errorMessage, requestId, requestMethod, requestUri);
      case 409 ->
          new ShareFileConflictException(
              errorCode, errorMessage, requestId, requestMethod, requestUri);
      case 429 ->
          new ShareFileRateLimitException(
              errorCode, errorMessage, requestId, requestMethod, requestUri, retryAfterSeconds);
      default -> {
        if (status >= 500) {
          yield new ShareFileServerException(
              status, errorCode, errorMessage, requestId, requestMethod, requestUri);
        }
        yield new ShareFileApiException(
            status, errorCode, errorMessage, requestId, requestMethod, requestUri);
      }
    };
  }
}
