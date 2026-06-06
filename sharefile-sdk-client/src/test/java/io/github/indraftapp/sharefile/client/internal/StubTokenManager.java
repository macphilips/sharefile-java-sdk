package io.github.indraftapp.sharefile.client.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.indraftapp.sharefile.client.auth.InMemoryTokenStore;
import io.github.indraftapp.sharefile.client.auth.TokenManager;
import io.github.indraftapp.sharefile.client.config.ShareFileConfig;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.core.model.OAuthToken;
import java.time.Instant;

/**
 * Factory for creating a {@link TokenManager} pre-loaded with a valid token for testing. Avoids
 * needing a real OAuth flow.
 */
final class StubTokenManager {

  private StubTokenManager() {}

  /** Creates a TokenManager with a pre-existing valid access token. */
  static TokenManager create(String accessToken) {
    OAuthToken token = new OAuthToken();
    token.setAccessToken(accessToken);
    token.setRefreshToken("test-refresh-token");
    token.setExpiresIn(36000L);
    token.setExpiresAt(Instant.now().plusSeconds(36000));

    return new TokenManager(
        ShareFileConfig.builder().subdomain("test").build(),
        "test-client-id",
        "test-client-secret",
        () ->
            Credentials.builder()
                .clientCredentials("test-client-id", "test-client-secret")
                .passwordGrant("user", "pass")
                .build(),
        new NoopTransport(),
        new InMemoryTokenStore(),
        new ObjectMapper(),
        token);
  }

  /** Minimal transport that should never be called in stub usage. */
  private static final class NoopTransport implements HttpTransport {
    @Override
    public HttpResponse execute(HttpRequest request) {
      throw new UnsupportedOperationException("StubTokenManager should not make HTTP calls");
    }
  }
}
