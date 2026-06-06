package io.github.indraftapp.sharefile.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.core.model.OAuthToken;
import org.junit.jupiter.api.Test;

/** Tests for {@link InMemoryTokenStore}. */
class InMemoryTokenStoreTest {

  @Test
  void initiallyEmpty() {
    var store = new InMemoryTokenStore();
    assertTrue(store.load().isEmpty());
  }

  @Test
  void saveAndLoad() {
    var store = new InMemoryTokenStore();
    OAuthToken token = createToken("access-1", "refresh-1");

    store.save(token);

    assertTrue(store.load().isPresent());
    assertSame(token, store.load().get());
  }

  @Test
  void saveOverwritesPrevious() {
    var store = new InMemoryTokenStore();
    OAuthToken token1 = createToken("access-1", "refresh-1");
    OAuthToken token2 = createToken("access-2", "refresh-2");

    store.save(token1);
    store.save(token2);

    assertEquals("access-2", store.load().get().getAccessToken());
  }

  @Test
  void clearRemovesToken() {
    var store = new InMemoryTokenStore();
    store.save(createToken("access-1", "refresh-1"));

    store.clear();

    assertTrue(store.load().isEmpty());
  }

  private static OAuthToken createToken(String accessToken, String refreshToken) {
    OAuthToken token = new OAuthToken();
    token.setAccessToken(accessToken);
    token.setRefreshToken(refreshToken);
    return token;
  }
}
