package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ShareFileClientBuilderTest {

  @Test
  void authorizationCodeBuilderBuildsSuccessfully() {
    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .authorizationCode("auth-code")
            .httpTransport(new ClientTestSupport.TestTransport())
            .build()) {
      assertNotNull(client);
      assertNotNull(client.items());
    }
  }

  @Test
  void authorizationCodeRedirectUriBuilderBuildsSuccessfully() {
    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .authorizationCodeRedirectUri("https://example.com/callback?code=auth-code&h=signature")
            .httpTransport(new ClientTestSupport.TestTransport())
            .build()) {
      assertNotNull(client);
      assertNotNull(client.items());
    }
  }

  @Test
  void dynamicCredentialProviderRemainsLazyDuringBuild() {
    AtomicInteger calls = new AtomicInteger();
    CredentialProvider provider =
        () -> {
          calls.incrementAndGet();
          return Credentials.builder()
              .clientCredentials("client-id", "client-secret")
              .passwordGrant("user@example.com", "pass123")
              .build();
        };

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .credentialProvider(provider)
            .httpTransport(new ClientTestSupport.TestTransport())
            .build()) {
      assertNotNull(client);
      assertEquals(0, calls.get());
    }
  }

  @Test
  void buildRejectsMissingSubdomain() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                ShareFileClient.builder()
                    .clientCredentials("client-id", "client-secret")
                    .authorizationCode("auth-code")
                    .build());

    assertEquals("subdomain must be configured", ex.getMessage());
  }

  @Test
  void buildRejectsMissingAuthConfiguration() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> ShareFileClient.builder().subdomain("testco").build());

    assertEquals("One authentication approach must be configured", ex.getMessage());
  }

  @Test
  void buildRejectsInvalidAuthCombinations() {
    assertThrows(
        IllegalStateException.class,
        () ->
            ShareFileClient.builder()
                .subdomain("testco")
                .clientCredentials("client-id", "client-secret")
                .build());

    assertThrows(
        IllegalStateException.class,
        () ->
            ShareFileClient.builder()
                .subdomain("testco")
                .authorizationCode("code")
                .passwordGrant("user", "pass")
                .clientCredentials("client-id", "client-secret")
                .build());

    assertThrows(
        IllegalStateException.class,
        () ->
            ShareFileClient.builder().subdomain("testco").accessToken("seeded", "refresh").build());

    assertThrows(
        IllegalStateException.class,
        () ->
            ShareFileClient.builder()
                .subdomain("testco")
                .credentialProvider(
                    () ->
                        Credentials.builder()
                            .clientCredentials("client-id", "client-secret")
                            .passwordGrant("user", "pass")
                            .build())
                .authorizationCode("code")
                .clientCredentials("client-id", "client-secret")
                .build());
  }

  @Test
  void baseUrlOverrideRoutesApiCallsToOverride() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Id\":\"home\",\"Name\":\"Home\"}");

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(transport)
            .baseUrl("https://wiremock.local/sf/v3")
            .build()) {
      client.items().getById("home");
    }

    assertEquals(
        "https://wiremock.local/sf/v3/Items(home)", transport.getLastRequest().uri().toString());
  }
}
