package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.AccessControl;
import io.github.indraftapp.sharefile.core.model.AccountUser;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.OperationResult;
import io.github.indraftapp.sharefile.core.model.request.BulkAccessControlRequest;
import io.github.indraftapp.sharefile.core.model.response.AccessControlBulkResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccessControlsClientTest {

  @Test
  void getByIdUsesCompositeKeyFormat() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.AccessControl",
          "Id": "acl-1",
          "CanView": true
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      AccessControl acl = context.accessControlsClient().getById("user-1", "item-2");

      assertEquals(Boolean.TRUE, acl.getCanView());
      assertEquals(
          ClientTestSupport.BASE_URL + "/AccessControls(principalid=user-1,itemid=item-2)",
          transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void getByItemUsesItemsAccessControlsEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "value": [
            {
              "odata.type": "ShareFile.Api.Models.AccessControl",
              "Id": "acl-1",
              "CanDownload": true
            }
          ]
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ODataFeed<AccessControl> feed = context.accessControlsClient().getByItem("folder-1");

      assertEquals(1, feed.getItems().size());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(folder-1)/AccessControls",
          transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void createReturnsCompletedResultForEntityPayload() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.AccessControl",
          "Id": "acl-1",
          "CanView": true
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      AccessControl request = new AccessControl();
      request.setCanView(Boolean.TRUE);

      OperationResult<AccessControl> result =
          context.accessControlsClient().create("folder-1", request, false);

      assertInstanceOf(OperationResult.Completed.class, result);
      assertEquals(Boolean.TRUE, result.getEntityOrThrow().getCanView());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(folder-1)/AccessControls?recursive=false",
          transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void updateReturnsPendingResultForAsyncPayload() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.AsyncOperation",
          "Id": "op-1",
          "State": "Queued"
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      AccessControl request = new AccessControl();
      request.setCanManagePermissions(Boolean.TRUE);

      OperationResult<AccessControl> result =
          context.accessControlsClient().update("folder-1", request, true);

      assertInstanceOf(OperationResult.Pending.class, result);
      assertEquals("op-1", result.asyncOperation().orElseThrow().getId());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(folder-1)/AccessControls?recursive=true",
          transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void bulkAndNotifyEndpointsMatchSpec() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"Succeeded\":2,\"Failed\":0,\"Errors\":[]}");
    transport.enqueueJsonResponse(200, "{\"Succeeded\":1,\"Failed\":0,\"Errors\":[]}");
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(204, "");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      BulkAccessControlRequest bulkRequest = new BulkAccessControlRequest();
      AccessControl acl = new AccessControl();
      acl.setCanDownload(Boolean.TRUE);
      bulkRequest.setAccessControls(List.of(acl));
      bulkRequest.setNotifyUser(Boolean.TRUE);
      bulkRequest.setNotifyMessage("granted");

      AccessControlBulkResult bulkSet =
          context.accessControlsClient().bulkSet("item-1", bulkRequest);
      AccessControlBulkResult bulkSetForPrincipal =
          context.accessControlsClient().bulkSetForPrincipal("principal-9", bulkRequest);
      context
          .accessControlsClient()
          .clone("folder-2", "principal-9", List.of("target-1", "target-2"));
      context.accessControlsClient().bulkDelete("item-1", List.of("principal-1", "principal-2"));
      context.accessControlsClient().notifyUsers("item-1", List.of("user-1", "user-2"), "hello");

      assertEquals(2, bulkSet.getSucceeded());
      assertEquals(1, bulkSetForPrincipal.getSucceeded());

      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(item-1)/AccessControls/BulkSet",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL
              + "/AccessControls/BulkSetForPrincipal?principalId=principal-9",
          transport.requests.get(1).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/AccessControls/Clone",
          transport.requests.get(2).uri().toString());
      assertTrue(transport.requests.get(2).body().contains("\"FolderId\":\"folder-2\""));
      assertTrue(transport.requests.get(2).body().contains("\"PrincipalId\":\"principal-9\""));
      assertTrue(
          transport
              .requests
              .get(2)
              .body()
              .contains("\"ClonePrincipalIds\":[\"target-1\",\"target-2\"]"));
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(item-1)/AccessControls/BulkDelete",
          transport.requests.get(3).uri().toString());
      assertEquals("[\"principal-1\",\"principal-2\"]", transport.requests.get(3).body());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Items(item-1)/AccessControls/NotifyUsers",
          transport.requests.get(4).uri().toString());
      assertTrue(transport.requests.get(4).body().contains("\"UserIds\":[\"user-1\",\"user-2\"]"));
      assertTrue(transport.requests.get(4).body().contains("\"Message\":\"hello\""));
    }
  }

  @Test
  void retryPolicyOverloadAllowsRetryForCreate() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        500, "{\"code\":\"InternalError\",\"message\":{\"value\":\"temporary failure\"}}");
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.AccessControl",
          "Id": "acl-2",
          "CanUpload": true
        }
        """);

    try (ClientTestSupport.TestContext context =
        ClientTestSupport.createContext(transport, RetryConfig.builder().maxRetries(0).build())) {
      AccessControl request = new AccessControl();
      AccountUser principal = new AccountUser();
      principal.setId("user-1");
      request.setPrincipal(principal);
      request.setCanUpload(Boolean.TRUE);

      OperationResult<AccessControl> result =
          context
              .accessControlsClient()
              .create("item-1", request, false, RetryPolicy.retryOnServerError(1));

      assertInstanceOf(OperationResult.Completed.class, result);
      assertEquals(2, transport.requests.size());
    }
  }
}
