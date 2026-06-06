package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.core.exception.ShareFileTimeoutException;
import io.github.indraftapp.sharefile.core.model.AsyncOperation;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import org.junit.jupiter.api.Test;

class AsyncOperationsClientTest {

  @Test
  void getByIdUsesEntityEndpoint() {
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
      AsyncOperation operation = context.asyncOperationsClient().getById("op-1");

      assertEquals("Queued", operation.getState());
      assertEquals(
          ClientTestSupport.BASE_URL + "/AsyncOperations(op-1)",
          transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void listUsesCollectionEndpointAndODataQuery() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "value": [
            {
              "odata.type": "ShareFile.Api.Models.AsyncOperation",
              "Id": "op-1",
              "State": "Queued"
            }
          ]
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ODataFeed<AsyncOperation> feed =
          context.asyncOperationsClient().list(ODataQuery.builder().top(25).build());

      assertEquals(1, feed.getItems().size());
      assertEquals(
          ClientTestSupport.BASE_URL + "/AsyncOperations?%24top=25",
          transport.getLastRequest().uri().toString());
    }
  }

  @Test
  void awaitCompletionReturnsImmediatelyForTerminalOperation() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.AsyncOperation",
          "Id": "op-1",
          "State": "Completed"
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      AsyncOperation operation =
          context.asyncOperationsClient().awaitCompletion("op-1", java.time.Duration.ofSeconds(2));

      assertEquals("Completed", operation.getState());
      assertEquals(1, transport.requests.size());
    }
  }

  @Test
  void awaitCompletionPollsUntilTerminalState() {
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
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.AsyncOperation",
          "Id": "op-1",
          "State": "InProgress"
        }
        """);
    transport.enqueueJsonResponse(
        200,
        """
        {
          "odata.type": "ShareFile.Api.Models.AsyncOperation",
          "Id": "op-1",
          "State": "Completed"
        }
        """);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      AsyncOperation operation =
          context.asyncOperationsClient().awaitCompletion("op-1", java.time.Duration.ofSeconds(3));

      assertEquals("Completed", operation.getState());
      assertEquals(3, transport.requests.size());
    }
  }

  @Test
  void awaitCompletionThrowsOnTimeout() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    String queuedResponse =
        """
        {
          "odata.type": "ShareFile.Api.Models.AsyncOperation",
          "Id": "op-1",
          "State": "Queued"
        }
        """;
    for (int i = 0; i < 5; i++) {
      transport.enqueueJsonResponse(200, queuedResponse);
    }

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ShareFileTimeoutException exception =
          assertThrows(
              ShareFileTimeoutException.class,
              () ->
                  context
                      .asyncOperationsClient()
                      .awaitCompletion("op-1", java.time.Duration.ofMillis(50)));

      assertTrue(exception.getMessage().contains("op-1"));
    }
  }
}
