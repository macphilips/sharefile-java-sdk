package io.github.indraftapp.sharefile.client.auth;

import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import java.util.Objects;

/**
 * A {@link CredentialProvider} that wraps fixed credentials.
 *
 * <p>Created internally when using the builder convenience methods ({@code clientCredentials()},
 * {@code passwordGrant()}, {@code authorizationCode()}).
 */
public final class StaticCredentialProvider implements CredentialProvider {

  private final Credentials credentials;

  /**
   * Creates a provider that always returns the given credentials.
   *
   * @param credentials the fixed credentials to return
   * @throws NullPointerException if credentials is null
   */
  public StaticCredentialProvider(Credentials credentials) {
    this.credentials = Objects.requireNonNull(credentials, "credentials must not be null");
  }

  @Override
  public Credentials resolve() {
    return credentials;
  }
}
