package io.github.indraftapp.sharefile.core.exception;

/** Thrown when a {@code CredentialProvider} fails to resolve credentials. */
public class CredentialResolutionException extends ShareFileAuthenticationException {

  public CredentialResolutionException(String message) {
    super(message);
  }

  public CredentialResolutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
