package io.github.indraftapp.sharefile.client.spi;

import io.github.indraftapp.sharefile.core.model.OAuthToken;
import java.util.Optional;

/**
 * SPI for persisting OAuth tokens across SDK restarts.
 *
 * <p>The default implementation ({@link
 * io.github.indraftapp.sharefile.client.auth.InMemoryTokenStore}) stores tokens in memory only.
 * Applications can provide custom implementations for Redis, JDBC, or other persistence backends.
 */
public interface TokenStore {

  /**
   * Saves the given token. Overwrites any previously stored token.
   *
   * @param token the OAuth token to persist
   */
  void save(OAuthToken token);

  /**
   * Loads the most recently saved token.
   *
   * @return the stored token, or empty if no token has been saved
   */
  Optional<OAuthToken> load();

  /** Removes any stored token. */
  void clear();
}
