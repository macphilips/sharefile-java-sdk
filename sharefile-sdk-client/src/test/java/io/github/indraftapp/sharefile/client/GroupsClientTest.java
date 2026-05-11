package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.Contact;
import io.github.indraftapp.sharefile.core.model.Group;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import org.junit.jupiter.api.Test;

class GroupsClientTest {

  @Test
  void getByIdAndDeleteUseEntityEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Id\":\"group-1\",\"Name\":\"Engineering\"}");
    transport.enqueueResponse(204, new byte[0]);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Group group = context.groupsClient().getById("group-1");
      context.groupsClient().delete("group-1");

      assertEquals("Engineering", group.getName());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups(group-1)",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups(group-1)",
          transport.requests.get(1).uri().toString());
      assertEquals("DELETE", transport.requests.get(1).method());
    }
  }

  @Test
  void listAndGetMembersAppendODataQueryParameters() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"value\":[]}");
    transport.enqueueJsonResponse(200, "{\"value\":[]}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ODataFeed<Group> groups = context.groupsClient().list(ODataQuery.builder().top(25).build());
      ODataFeed<Contact> members =
          context.groupsClient().getMembers("group-1", ODataQuery.builder().top(10).build());

      assertEquals(0, groups.getItems().size());
      assertEquals(0, members.getItems().size());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups?%24top=25",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups(group-1)/Contacts?%24top=10",
          transport.requests.get(1).uri().toString());
    }
  }

  @Test
  void createAndUpdateUseConfiguredRetryAwareEndpoints() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Id\":\"group-1\",\"Name\":\"Engineering\"}");
    transport.enqueueJsonResponse(200, "{\"Id\":\"group-1\",\"Name\":\"Platform\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Group payload = new Group();
      payload.setName("Engineering");
      Group created = context.groupsClient().create(payload, RetryPolicy.retryOnServerError(1));

      Group update = new Group();
      update.setName("Platform");
      Group updated =
          context.groupsClient().update("group-1", update, RetryPolicy.retryOnServerError(1));

      assertEquals("Engineering", created.getName());
      assertEquals("Platform", updated.getName());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups", transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups(group-1)",
          transport.requests.get(1).uri().toString());
      assertEquals("{\"Name\":\"Engineering\"}", transport.requests.get(0).body());
      assertEquals("{\"Name\":\"Platform\"}", transport.requests.get(1).body());
    }
  }

  @Test
  void addAndRemoveMemberUseNestedContactsEndpoints() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Id\":\"user-1\",\"Email\":\"user@example.com\"}");
    transport.enqueueResponse(204, new byte[0]);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Contact contact =
          context.groupsClient().addMember("group-1", "user-1", RetryPolicy.retryOnServerError(1));
      context.groupsClient().removeMember("group-1", "user-1");

      assertEquals("user-1", contact.getId());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups(group-1)/Contacts",
          transport.requests.get(0).uri().toString());
      assertEquals("{\"Id\":\"user-1\"}", transport.requests.get(0).body());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups(group-1)/Contacts",
          transport.requests.get(1).uri().toString());
      assertEquals("DELETE", transport.requests.get(1).method());
      assertEquals("[{\"Id\":\"user-1\"}]", transport.requests.get(1).body());
    }
  }

  @Test
  void exportContactsUsesExportDocumentEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    byte[] responseBody =
        "id,name\nuser-1,Test User\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    transport.enqueueResponse(200, responseBody);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      byte[] exported = context.groupsClient().exportContacts("group-1");

      assertEquals(
          ClientTestSupport.BASE_URL + "/Groups(group-1)/ExportDocument",
          transport.getLastRequest().uri().toString());
      assertEquals("GET", transport.getLastRequest().method());
      assertEquals(
          "id,name\nuser-1,Test User\n",
          new String(exported, java.nio.charset.StandardCharsets.UTF_8));
    }
  }
}
