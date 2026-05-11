package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.indraftapp.sharefile.core.model.Session;
import org.junit.jupiter.api.Test;

class SessionsClientTest {

  @Test
  void getUsesCollectionEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Id\":\"session-1\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Session session = context.sessionsClient().get();

      assertEquals("session-1", session.getId());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Sessions", transport.getLastRequest().uri().toString());
      assertEquals("GET", transport.getLastRequest().method());
    }
  }

  @Test
  void loginPostsSessionPayloadToCollectionEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Id\":\"session-2\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Session payload = new Session();
      payload.setId("session-2");

      Session session = context.sessionsClient().login(payload);

      assertEquals("session-2", session.getId());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Sessions", transport.getLastRequest().uri().toString());
      assertEquals("POST", transport.getLastRequest().method());
      assertEquals("{\"Id\":\"session-2\"}", transport.getLastRequest().body());
    }
  }

  @Test
  void logoutDeletesCollectionEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueResponse(204, new byte[0]);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      context.sessionsClient().logout();

      assertEquals(
          ClientTestSupport.BASE_URL + "/Sessions", transport.getLastRequest().uri().toString());
      assertEquals("DELETE", transport.getLastRequest().method());
    }
  }
}
