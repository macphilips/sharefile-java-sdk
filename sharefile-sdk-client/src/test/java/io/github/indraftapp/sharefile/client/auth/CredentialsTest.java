package io.github.indraftapp.sharefile.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.core.model.enums.GrantType;
import org.junit.jupiter.api.Test;

/** Tests for the {@link Credentials} record and builder. */
class CredentialsTest {

  @Test
  void passwordGrantCreatesValidCredentials() {
    Credentials creds =
        Credentials.builder()
            .clientCredentials("client-id", "client-secret")
            .passwordGrant("user@example.com", "pass123")
            .build();

    assertEquals("client-id", creds.clientId());
    assertEquals("client-secret", creds.clientSecret());
    assertEquals(GrantType.PASSWORD, creds.grantType());
    assertEquals("user@example.com", creds.username());
    assertEquals("pass123", creds.password());
    assertNull(creds.authorizationCode());
  }

  @Test
  void authorizationCodeGrantCreatesValidCredentials() {
    Credentials creds =
        Credentials.builder()
            .clientCredentials("client-id", "client-secret")
            .authorizationCode("auth-code-123")
            .build();

    assertEquals("client-id", creds.clientId());
    assertEquals("client-secret", creds.clientSecret());
    assertEquals(GrantType.AUTHORIZATION_CODE, creds.grantType());
    assertEquals("auth-code-123", creds.authorizationCode());
    assertNull(creds.username());
    assertNull(creds.password());
  }

  @Test
  void missingClientIdThrows() {
    assertThrows(
        NullPointerException.class,
        () -> Credentials.builder().passwordGrant("user", "pass").build());
  }

  @Test
  void missingClientSecretThrows() {
    assertThrows(
        NullPointerException.class,
        () -> new Credentials("id", null, GrantType.PASSWORD, "user", "pass", null));
  }

  @Test
  void passwordGrantMissingUsernameThrows() {
    assertThrows(
        NullPointerException.class,
        () -> new Credentials("id", "secret", GrantType.PASSWORD, null, "pass", null));
  }

  @Test
  void passwordGrantMissingPasswordThrows() {
    assertThrows(
        NullPointerException.class,
        () -> new Credentials("id", "secret", GrantType.PASSWORD, "user", null, null));
  }

  @Test
  void authorizationCodeGrantMissingCodeThrows() {
    assertThrows(
        NullPointerException.class,
        () -> new Credentials("id", "secret", GrantType.AUTHORIZATION_CODE, null, null, null));
  }

  @Test
  void refreshTokenGrantTypeThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Credentials("id", "secret", GrantType.REFRESH_TOKEN, null, null, null));
  }

  @Test
  void builderOverridesGrantType() {
    // If passwordGrant then authorizationCode are called, last one wins
    Credentials creds =
        Credentials.builder()
            .clientCredentials("id", "secret")
            .passwordGrant("user", "pass")
            .authorizationCode("code123")
            .build();

    assertEquals(GrantType.AUTHORIZATION_CODE, creds.grantType());
    assertEquals("code123", creds.authorizationCode());
  }
}
