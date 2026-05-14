package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.core.exception.ShareFileAuthenticationException;
import io.github.indraftapp.sharefile.core.model.HealthStatus;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class ShareFileClientTest {

  @Test
  void accessorsReturnCachedSingletonClients() {
    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(new ClientTestSupport.TestTransport())
            .build()) {
      assertSame(client.items(), client.items());
      assertSame(client.users(), client.users());
      assertSame(client.shares(), client.shares());
      assertSame(client.accessControls(), client.accessControls());
      assertSame(client.asyncOperations(), client.asyncOperations());
      assertSame(client.transfers(), client.transfers());
    }
  }

  @Test
  void placeholderAccessorsReturnNonNullClients() {
    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(new ClientTestSupport.TestTransport())
            .build()) {
      assertNotNull(client.groups());
      assertNotNull(client.accounts());
      assertNotNull(client.zones());
      assertNotNull(client.webhookSubscriptions());
      assertNotNull(client.sessions());
    }
  }

  @Test
  void accountsGetUsesAccountsEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Subdomain\":\"testco\"}");

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(transport)
            .build()) {
      client.accounts().get();
    }

    assertEquals(
        ClientTestSupport.BASE_URL + "/Accounts", transport.getLastRequest().uri().toString());
  }

  @Test
  void groupsAccessorReturnsUsableRealClient() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Id\":\"group-1\",\"Name\":\"Engineering\"}");

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(transport)
            .build()) {
      assertEquals("Engineering", client.groups().getById("group-1").getName());
    }

    assertEquals(
        ClientTestSupport.BASE_URL + "/Groups(group-1)",
        transport.getLastRequest().uri().toString());
  }

  @Test
  void webhookSubscriptionsAccessorReturnsUsableRealClient() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200, "{\"Id\":\"sub-1\",\"WebhookUrl\":\"https://example.test\"}");

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(transport)
            .build()) {
      assertEquals(
          "https://example.test", client.webhookSubscriptions().getById("sub-1").getWebhookUrl());
    }

    assertEquals(
        ClientTestSupport.BASE_URL + "/WebhookSubscriptions(sub-1)",
        transport.getLastRequest().uri().toString());
  }

  @Test
  void sessionsAccessorReturnsUsableRealClient() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Id\":\"session-1\"}");

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(transport)
            .build()) {
      assertEquals("session-1", client.sessions().get().getId());
    }

    assertEquals(
        ClientTestSupport.BASE_URL + "/Sessions", transport.getLastRequest().uri().toString());
  }

  @Test
  void checkHealthReturnsUpWhenAccountLookupSucceeds() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Subdomain\":\"healthco\"}");

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(transport)
            .build()) {
      HealthStatus status = client.checkHealth();

      assertTrue(status.up());
      assertEquals("healthco", status.subdomain());
      assertEquals(0L, status.tokenExpiresInSeconds());
    }
  }

  @Test
  void checkHealthReturnsDownWhenAccountLookupFails() {
    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(new FailingTransport())
            .build()) {
      HealthStatus status = client.checkHealth();

      assertFalse(status.up());
      assertNotNull(status.error());
    }
  }

  @Test
  void reauthenticateUsesRefreshWhenAvailable() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "access_token": "refreshed-access",
          "refresh_token": "refreshed-refresh",
          "token_type": "bearer",
          "expires_in": 3600
        }
        """);

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(transport)
            .build()) {
      client.reauthenticate();
    }

    assertEquals(1, transport.requests.size());
    assertTrue(transport.getLastRequest().body().contains("grant_type=refresh_token"));
  }

  @Test
  void reauthenticateWithCredentialsSwitchesToNewStaticAuthSource() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "access_token": "rotated-access",
          "refresh_token": "rotated-refresh",
          "token_type": "bearer",
          "expires_in": 3600
        }
        """);
    transport.enqueueJsonResponse(
        200,
        """
        {
          "access_token": "second-access",
          "refresh_token": "second-refresh",
          "token_type": "bearer",
          "expires_in": 3600
        }
        """);

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", null)
            .httpTransport(transport)
            .build()) {
      client.reauthenticate(
          Credentials.builder()
              .clientCredentials("rotated-client-id", "rotated-client-secret")
              .passwordGrant("rotated-user@example.com", "rotated-pass")
              .build());
      client.reauthenticate();
    }

    assertEquals(2, transport.requests.size());
    assertTrue(transport.requests.get(0).body().contains("client_id=rotated-client-id"));
    assertTrue(transport.requests.get(1).body().contains("client_id=rotated-client-id"));
  }

  @Test
  void reauthenticateWithProviderSwitchesToDynamicAuthSource() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "access_token": "provider-access",
          "refresh_token": "provider-refresh",
          "token_type": "bearer",
          "expires_in": 3600
        }
        """);

    CredentialProvider provider =
        () ->
            Credentials.builder()
                .clientCredentials("provider-client-id", "provider-client-secret")
                .passwordGrant("provider-user@example.com", "provider-pass")
                .build();

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", null)
            .httpTransport(transport)
            .build()) {
      client.reauthenticate(provider);
    }

    assertEquals(1, transport.requests.size());
    assertTrue(transport.getLastRequest().body().contains("client_id=provider-client-id"));
  }

  @Test
  void seededTokenOnlyReauthenticateFailsButCredentialsRecoveryWorks() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();

    try (ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", null)
            .httpTransport(transport)
            .build()) {
      assertThrows(ShareFileAuthenticationException.class, client::reauthenticate);

      transport.enqueueJsonResponse(
          200,
          """
          {
            "access_token": "recovered-access",
            "refresh_token": "recovered-refresh",
            "token_type": "bearer",
            "expires_in": 3600
          }
          """);

      client.reauthenticate(
          Credentials.builder()
              .clientCredentials("replacement-client-id", "replacement-client-secret")
              .passwordGrant("replacement-user@example.com", "replacement-pass")
              .build());
    }

    assertEquals(1, transport.requests.size());
    assertTrue(transport.getLastRequest().body().contains("client_id=replacement-client-id"));
  }

  @Test
  void closeLeavesCallerProvidedExecutorAndTransportUntouched() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    CloseTrackingTransport transport = new CloseTrackingTransport();

    ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .executor(executor)
            .httpTransport(transport)
            .build();

    assertDoesNotThrow(client::close);
    assertDoesNotThrow(client::close);
    assertFalse(executor.isShutdown());
    assertFalse(transport.closed);

    executor.shutdownNow();
  }

  @Test
  void closeShutsDownSdkOwnedExecutor() throws Exception {
    ShareFileClient client =
        ShareFileClient.builder()
            .subdomain("testco")
            .clientCredentials("client-id", "client-secret")
            .accessToken("seeded-token", "seeded-refresh")
            .httpTransport(new CloseTrackingTransport())
            .build();

    ExecutorService executor = (ExecutorService) readField(client, "executor");
    assertFalse(executor.isShutdown());

    client.close();

    assertTrue(executor.isShutdown());
  }

  private static Object readField(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static final class FailingTransport implements HttpTransport {
    @Override
    public HttpResponse execute(HttpRequest request) {
      throw new IllegalStateException("boom");
    }
  }

  private static final class CloseTrackingTransport implements HttpTransport, AutoCloseable {
    boolean closed;

    @Override
    public HttpResponse execute(HttpRequest request) {
      throw new UnsupportedOperationException("No requests expected");
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
