package io.github.indraftapp.sharefile.core.exception;

/**
 * Thrown when HMAC validation of an authorization code redirect fails.
 *
 * <p>This indicates that the redirect URI signature does not match the expected HMAC-SHA256 digest,
 * which may indicate a tampered redirect or invalid client secret.
 */
public class HmacValidationException extends ShareFileAuthenticationException {

  public HmacValidationException(String message) {
    super(message);
  }

  public HmacValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
