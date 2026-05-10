package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.core.model.Contact;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.Share;
import io.github.indraftapp.sharefile.core.model.request.RequestShareRequest;
import io.github.indraftapp.sharefile.core.model.request.SendShareRequest;
import io.github.indraftapp.sharefile.core.model.request.ShareNotificationRequest;
import io.github.indraftapp.sharefile.core.model.response.DownloadSpecification;
import java.util.List;
import org.junit.jupiter.api.Test;

class SharesClientTest {

  @Test
  void createSendShareAndCreateRequestShareBothPostToShares() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.Share\",\"Id\":\"share-1\",\"Title\":\"Send\"}");
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.Share\",\"Id\":\"share-2\",\"Title\":\"Request\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      SendShareRequest send = new SendShareRequest();
      send.setRecipients(List.of("a@example.com"));
      send.setItems(List.of("item-1"));
      RequestShareRequest request = new RequestShareRequest();
      request.setRecipients(List.of("b@example.com"));
      request.setFolderID("folder-1");

      Share sendShare = context.sharesClient().createSendShare(send);
      Share requestShare = context.sharesClient().createRequestShare(request);

      assertEquals("share-1", sendShare.getId());
      assertEquals("share-2", requestShare.getId());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Shares", transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Shares", transport.requests.get(1).uri().toString());
      assertTrue(transport.requests.get(0).body().contains("\"ShareType\":\"Send\""));
      assertTrue(transport.requests.get(0).body().contains("\"Items\":[{\"Id\":\"item-1\"}]"));
      assertTrue(
          transport
              .requests
              .get(0)
              .body()
              .contains("\"Recipients\":[{\"User\":{\"Email\":\"a@example.com\"}}]"));
      assertTrue(transport.requests.get(1).body().contains("\"ShareType\":\"Request\""));
      assertTrue(transport.requests.get(1).body().contains("\"Parent\":{\"Id\":\"folder-1\"}"));
      assertTrue(
          transport
              .requests
              .get(1)
              .body()
              .contains("\"Recipients\":[{\"User\":{\"Email\":\"b@example.com\"}}]"));
    }
  }

  @Test
  void shareSpecificEndpointsMatchSpec() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"value\":[{\"odata.type\":\"ShareFile.Api.Models.Share\",\"Id\":\"share-1\",\"Title\":\"Mine\"}]}");
    transport.enqueueJsonResponse(
        200,
        "{\"value\":[{\"odata.type\":\"ShareFile.Api.Models.Contact\",\"Id\":\"contact-1\"}]}");
    transport.enqueueJsonResponse(204, "");
    transport.enqueueJsonResponse(
        200,
        "{\"value\":[{\"odata.type\":\"ShareFile.Api.Models.File\",\"Id\":\"item-1\",\"Name\":\"doc.pdf\"}]}");
    transport.enqueueJsonResponse(
        200,
        "{\"DownloadUrl\":\"https://download.example.com/file.zip\",\"PrepStatus\":\"Ready\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ODataFeed<Share> shares = context.sharesClient().getByUser("user-1");
      ODataFeed<Contact> recipients = context.sharesClient().getRecipients("share-1");
      ShareNotificationRequest notification = new ShareNotificationRequest();
      notification.setRecipients(List.of("c@example.com"));
      notification.setBody("hello");
      context.sharesClient().sendNotification("share-1", notification);
      ODataFeed<Item> items = context.sharesClient().getItems("share-1");
      DownloadSpecification download = context.sharesClient().downloadItems("share-1");

      assertEquals(1, shares.getItems().size());
      assertEquals(1, recipients.getItems().size());
      assertEquals(1, items.getItems().size());
      assertEquals("Ready", download.getPrepStatus());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Users(user-1)/Shares",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Shares(share-1)/Recipients",
          transport.requests.get(1).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Shares(share-1)/Notify",
          transport.requests.get(2).uri().toString());
      assertTrue(transport.requests.get(2).body().contains("\"Recipients\":[\"c@example.com\"]"));
      assertEquals(
          ClientTestSupport.BASE_URL + "/Shares(share-1)/Items",
          transport.requests.get(3).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/Shares(share-1)/Download?redirect=false",
          transport.requests.get(4).uri().toString());
    }
  }

  @Test
  void updateOmitsUnsetNullFieldsFromPatchPayload() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200,
        "{\"odata.type\":\"ShareFile.Api.Models.Share\",\"Id\":\"share-1\",\"Title\":\"Updated\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      Share share = new Share();
      share.setTitle("Updated");

      Share updated = context.sharesClient().update("share-1", share);

      assertEquals("Updated", updated.getTitle());
      assertEquals("PATCH", transport.getLastRequest().method());
      assertTrue(transport.getLastRequest().body().contains("\"Title\":\"Updated\""));
      assertFalse(transport.getLastRequest().body().contains("\"Body\":null"));
    }
  }
}
