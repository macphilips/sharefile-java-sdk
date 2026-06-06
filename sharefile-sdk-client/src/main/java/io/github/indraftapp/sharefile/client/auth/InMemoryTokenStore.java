package io.github.indraftapp.sharefile.client.auth;

import io.github.indraftapp.sharefile.client.spi.TokenStore;
import io.github.indraftapp.sharefile.core.model.OAuthToken;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default {@link TokenStore} that holds the token in memory.
 *
 * <p>Tokens are lost when the JVM exits. This is suitable for short-lived applications or when
 * token persistence is handled externally.
 *
 * <p>This implementation is thread-safe.
 */
public final class InMemoryTokenStore implements TokenStore {

  private final AtomicReference<OAuthToken> tokenRef = new AtomicReference<>();

  @Override
  public void save(OAuthToken token) {
    tokenRef.set(token);
  }

  @Override
  public Optional<OAuthToken> load() {
    return Optional.ofNullable(tokenRef.get());
  }

  @Override
  public void clear() {
    tokenRef.set(null);
  }
}
