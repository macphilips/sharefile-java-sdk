package io.github.indraftapp.sharefile.client.auth;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.indraftapp.sharefile.client.spi.Credentials;
import org.junit.jupiter.api.Test;

/** Tests for {@link StaticCredentialProvider}. */
class StaticCredentialProviderTest {

  @Test
  void resolveReturnsFixedCredentials() {
    Credentials creds =
        Credentials.builder()
            .clientCredentials("id", "secret")
            .passwordGrant("user", "pass")
            .build();

    var provider = new StaticCredentialProvider(creds);
    assertSame(creds, provider.resolve());
    // Returns same instance on repeated calls
    assertSame(creds, provider.resolve());
  }

  @Test
  void nullCredentialsThrows() {
    assertThrows(NullPointerException.class, () -> new StaticCredentialProvider(null));
  }
}
