package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.OperationResult;
import io.github.indraftapp.sharefile.core.model.request.FolderCreateRequest;
import io.github.indraftapp.sharefile.core.model.response.SearchResults;
import io.github.indraftapp.sharefile.core.odata.Filter;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemsClientTest {

  @Test
  void getByIdUsesEntityUriWithLiteralSpecialId() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200, "{\"odata.type\":\"ShareFile.Api.Models.Folder\",\"Id\":\"home\",\"Name\":\"Home\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Item item = context.itemsClient().getById("home");

      assertEquals("Home", item.getName());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(home)", transport.getLastRequest().uri().toString());
      assertEquals("GET", transport.getLastRequest().method());
    }
  }

  @Test
  void getChildrenAppendsODataQueryParameters() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.count": 1,
          "value": [
            {
              "odata.type": "ShareFile.Api.Models.File",
              "Id": "file-1",
              "Name": "alpha.txt"
            }
          ]
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ODataFeed<Item> feed =
          context
              .itemsClient()
              .getChildren(
                  "folder-1",
                  ODataQuery.builder().top(50).filter(Filter.eq("Name", "Reports")).build());

      assertEquals(1, feed.getItems().size());
      String uri = transport.getLastRequest().uri().toString();
      assertTrue(uri.startsWith(ClientTestSupport.BASE_URL + "/Items(folder-1)/Children?"));
      assertTrue(uri.contains("%24top=50"));
      assertTrue(uri.contains("%24filter=Name%20eq%20%27Reports%27"));
    }
  }

  @Test
  void getByPathEncodesExplicitPathParameter() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.Folder\",\"Id\":\"folder-1\",\"Name\":\"Reports\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Item item = context.itemsClient().getByPath("/Shared Documents/Quarterly Reports");

      assertEquals("Reports", item.getName());
      String uri = transport.getLastRequest().uri().toString();
      assertTrue(uri.startsWith(ClientTestSupport.BASE_URL + "/Items/ByPath?"));
      assertTrue(uri.contains("path=%2FShared%20Documents%2FQuarterly%20Reports"));
    }
  }

  @Test
  void versionsAndInfoUseDocumentedApiReferenceEndpoints() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "value": [
            {
              "odata.type": "ShareFile.Api.Models.File",
              "Id": "file-1",
              "Name": "v1.txt"
            }
          ]
        }
        """);
    transport.enqueueJsonResponse(200, "{\"FileCount\":1,\"ChildCount\":2}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ODataFeed<Item> versions = context.itemsClient().getVersions("file-1");
      context.itemsClient().getFolderAccessInfo("folder-2");

      assertEquals(1, versions.getItems().size());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(file-1)/Stream",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(folder-2)/Info",
          transport.requests.get(1).uri().toString());
    }
  }

  @Test
  void searchBuildsGlobalAndScopedEndpoints() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Results\":[],\"TotalCount\":0,\"TimedOut\":false}");
    transport.enqueueJsonResponse(200, "{\"Results\":[],\"TotalCount\":0,\"TimedOut\":false}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      SearchResults global = context.itemsClient().search("budget 2026", 25, 10);
      SearchResults scoped = context.itemsClient().search("folder-99", "budget 2026", 25, 10);

      assertEquals(0, global.getTotalCount());
      assertEquals(0, scoped.getTotalCount());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items/Search?query=budget%202026&maxResults=25&skip=10",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL
              + "/Items(folder-99)/Search?query=budget%202026&maxResults=25&skip=10",
          transport.requests.get(1).uri().toString());
    }
  }

  @Test
  void createFolderPostsRequestBodyToFolderEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.Folder\",\"Id\":\"folder-2\",\"Name\":\"New Folder\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      FolderCreateRequest request = new FolderCreateRequest();
      request.setName("New Folder");
      request.setDescription("Quarterly reports");
      request.setOverWrite(Boolean.TRUE);

      Item created = context.itemsClient().createFolder("parent-1", request);

      assertEquals("New Folder", created.getName());
      ClientTestSupport.RecordedRequest recorded = transport.getLastRequest();
      assertEquals("POST", recorded.method());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(parent-1)/Folder", recorded.uri().toString());
      assertTrue(recorded.body().contains("\"Name\":\"New Folder\""));
      assertTrue(recorded.body().contains("\"Description\":\"Quarterly reports\""));
      assertTrue(recorded.body().contains("\"OverWrite\":true"));
    }
  }

  @Test
  void updateReturnsCompletedOperationResultForEntityPayload() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.File",
          "Id": "file-1",
          "Name": "renamed.txt"
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Item request = new Item();
      request.setName("renamed.txt");

      OperationResult<Item> result = context.itemsClient().update("file-1", request);

      assertInstanceOf(OperationResult.Completed.class, result);
      assertEquals("renamed.txt", result.getEntityOrThrow().getName());
      assertEquals("PATCH", transport.getLastRequest().method());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(file-1)",
          transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void copyReturnsPendingOperationResultForAsyncPayload() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.AsyncOperation",
          "Id": "op-123",
          "State": "Queued"
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      OperationResult<Item> result = context.itemsClient().copy("item-1", "target-2", true);

      assertInstanceOf(OperationResult.Pending.class, result);
      assertTrue(result.isAsync());
      assertEquals("op-123", result.asyncOperation().orElseThrow().getId());
      assertEquals("POST", transport.getLastRequest().method());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(item-1)/Copy?targetid=target-2&overwrite=true",
          transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void deleteAndBulkAndCheckInEndpointsMatchMatrix() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.File\",\"Id\":\"file-1\",\"Name\":\"locked.txt\"}");
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.File\",\"Id\":\"file-1\",\"Name\":\"locked.txt\"}");
    transport.enqueueJsonResponse(204, "");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ItemsClient items = context.itemsClient();

      items.delete("item-1");
      items.bulkDelete("parent-1", List.of("item-1", "item-2"), true);
      items.bulkRestore(List.of("item-3", "item-4"));
      items.checkOut("file-1");
      items.checkIn("file-1", "done");
      items.discardCheckOut("file-1");

      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(item-1)",
          transport.requests.get(0).uri().toString());
      assertEquals("DELETE", transport.requests.get(0).method());

      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(parent-1)/BulkDelete?deletePermanently=true",
          transport.requests.get(1).uri().toString());
      assertEquals("[\"item-1\",\"item-2\"]", transport.requests.get(1).body());

      assertEquals(
          ClientTestSupport.BASE_URL + "/Items/BulkRestore",
          transport.requests.get(2).uri().toString());
      assertTrue(transport.requests.get(2).body().contains("\"ids\":[\"item-3\",\"item-4\"]"));

      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(file-1)/CheckOut",
          transport.requests.get(3).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(file-1)/CheckIn",
          transport.requests.get(4).uri().toString());
      assertTrue(transport.requests.get(4).body().contains("\"Comment\":\"done\""));
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(file-1)/DiscardCheckOut",
          transport.requests.get(5).uri().toString());
    }
  }

  @Test
  void retryPolicyOverloadAllowsRetryForCreateFolder() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        500, "{\"code\":\"InternalError\",\"message\":{\"value\":\"temporary failure\"}}");
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.Folder\",\"Id\":\"folder-1\",\"Name\":\"Retry Folder\"}");

    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(transport, RetryConfig.builder().maxRetries(0).build())) {
      FolderCreateRequest request = new FolderCreateRequest();
      request.setName("Retry Folder");

      Item created =
          context
              .itemsClient()
              .createFolder("parent-1", request, RetryPolicy.retryOnServerError(1));

      assertEquals("Retry Folder", created.getName());
      assertEquals(2, transport.requests.size());
      assertFalse(transport.requests.get(0).headers().get("X-Request-Id").isBlank());
    }
  }
}
