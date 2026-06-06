package io.github.indraftapp.sharefile.client.spi;

import io.github.indraftapp.sharefile.core.exception.CredentialResolutionException;

/**
 * SPI for resolving OAuth2 credentials at authentication time.
 *
 * <p>Implementations can source credentials from any backend — environment variables, databases,
 * secret managers, or runtime user context. The SDK calls {@link #resolve()} only when it needs to
 * perform initial authentication or re-authenticate after a refresh-token failure. Normal token
 * refresh cycles never invoke this method.
 *
 * <p>This is a {@link FunctionalInterface}, so it can be implemented with a lambda:
 *
 * <pre>{@code
 * CredentialProvider provider = () -> Credentials.builder()
 *     .clientCredentials(System.getenv("SF_CLIENT_ID"), System.getenv("SF_CLIENT_SECRET"))
 *     .passwordGrant(System.getenv("SF_USER"), System.getenv("SF_PASS"))
 *     .build();
 * }</pre>
 *
 * @see Credentials
 */
@FunctionalInterface
public interface CredentialProvider {

  /**
   * Resolves the current credentials for authenticating with the ShareFile API.
   *
   * @return the resolved credentials, never {@code null}
   * @throws CredentialResolutionException if credentials cannot be resolved
   */
  Credentials resolve() throws CredentialResolutionException;
}
