package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.exception.ShareFileNotFoundException;
import io.github.indraftapp.sharefile.core.model.Group;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.OperationResult;
import io.github.indraftapp.sharefile.core.model.User;
import io.github.indraftapp.sharefile.core.model.response.UserPreferences;
import io.github.indraftapp.sharefile.core.model.response.UserSecurity;
import org.junit.jupiter.api.Test;

class UsersClientTest {

  @Test
  void getCurrentUserUsesUsersCollectionEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.User\",\"Id\":\"me\",\"Email\":\"me@example.com\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      User user = context.usersClient().getCurrentUser();

      assertEquals("me@example.com", user.getEmail());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users", transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void getByEmailUsesFilterQueryAndReturnsFirstMatch() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "value": [
            {
              "odata.type": "ShareFile.Api.Models.User",
              "Id": "user-1",
              "Email": "person@example.com"
            }
          ]
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      User user = context.usersClient().getByEmail("person@example.com");

      assertEquals("user-1", user.getId());
      String uri = transport.getLastRequest().uri().toString();
      assertTrue(uri.startsWith(ClientTestSupport.BASE_URL + "/Users?"));
      assertTrue(uri.contains("%24filter=Email%20eq%20%27person%40example.com%27"));
      assertTrue(uri.contains("%24top=1"));
    }
  }

  @Test
  void getByEmailThrowsWhenNoUserMatches() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"value\":[]}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      assertThrows(
          ShareFileNotFoundException.class,
          () -> context.usersClient().getByEmail("missing@example.com"));
    }
  }

  @Test
  void preferencesSecurityAndMembershipEndpointsMatchSpec() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Locale\":\"en-US\"}");
    transport.enqueueJsonResponse(200, "{\"Locale\":\"fr-FR\"}");
    transport.enqueueJsonResponse(200, "{\"IsDisabled\":false}");
    transport.enqueueJsonResponse(200, "{\"IsDisabled\":true}");
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(
        200, "{\"value\":[{\"odata.type\":\"ShareFile.Api.Models.Group\",\"Id\":\"group-1\"}]}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      UserPreferences preferences = context.usersClient().getPreferences("user-1");
      UserPreferences updatedPreferences = new UserPreferences();
      updatedPreferences.setProperty("Locale", "fr-FR");
      UserPreferences savedPreferences =
          context.usersClient().updatePreferences("user-1", updatedPreferences);

      UserSecurity security = context.usersClient().getSecurity("user-1");
      UserSecurity updatedSecurity = new UserSecurity();
      updatedSecurity.setProperty("IsDisabled", true);
      UserSecurity savedSecurity = context.usersClient().updateSecurity("user-1", updatedSecurity);

      context.usersClient().resetPassword("user-1");
      context.usersClient().sendWelcomeEmail("user-1");
      ODataFeed<Group> groups = context.usersClient().getGroups("user-1");

      assertEquals("en-US", preferences.getProperty("Locale"));
      assertEquals("fr-FR", savedPreferences.getProperty("Locale"));
      assertEquals(Boolean.FALSE, security.getProperty("IsDisabled"));
      assertEquals(Boolean.TRUE, savedSecurity.getProperty("IsDisabled"));
      assertEquals(1, groups.getItems().size());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/Preferences",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/Preferences",
          transport.requests.get(1).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/Security",
          transport.requests.get(2).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/Security",
          transport.requests.get(3).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/ResetPassword",
          transport.requests.get(4).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/ResendWelcome",
          transport.requests.get(5).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/Groups",
          transport.requests.get(6).uri().toString());
    }
  }

  @Test
  void updateOmitsUnsetNullFieldsFromPatchPayload() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.User\",\"Id\":\"user-1\",\"Email\":\"person@example.com\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      User update = new User();
      update.setEmail("person@example.com");

      OperationResult<User> saved = context.usersClient().update("user-1", update);

      assertEquals("person@example.com", saved.getEntityOrThrow().getEmail());
      assertEquals("PATCH", transport.getLastRequest().method());
      assertTrue(transport.getLastRequest().body().contains("\"Email\":\"person@example.com\""));
      assertFalse(transport.getLastRequest().body().contains("\"FirstName\":null"));
    }
  }

  @Test
  void createAndUpdateSupportRetryPolicyOverloads() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.User\",\"Id\":\"user-1\",\"Email\":\"created@example.com\"}");
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.User\",\"Id\":\"user-1\",\"Email\":\"updated@example.com\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      User user = new User();
      user.setEmail("created@example.com");

      User created = context.usersClient().create(user, RetryPolicy.retryOnServerError(1));

      User updatedUser = new User();
      updatedUser.setEmail("updated@example.com");
      OperationResult<User> updated =
          context.usersClient().update("user-1", updatedUser, RetryPolicy.retryOnServerError(1));

      assertEquals("created@example.com", created.getEmail());
      assertEquals("updated@example.com", updated.getEntityOrThrow().getEmail());
      assertEquals("POST", transport.requests.get(0).method());
      assertEquals("PATCH", transport.requests.get(1).method());
    }
  }

  @Test
  void resetPasswordAndSendWelcomeEmailSupportRetryPolicyOverloads() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(204, "");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      RetryPolicy policy = RetryPolicy.retryOnServerError(1);

      context.usersClient().resetPassword("user-1", policy);
      context.usersClient().sendWelcomeEmail("user-1", policy);

      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/ResetPassword",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/ResendWelcome",
          transport.requests.get(1).uri().toString());
    }
  }
}
