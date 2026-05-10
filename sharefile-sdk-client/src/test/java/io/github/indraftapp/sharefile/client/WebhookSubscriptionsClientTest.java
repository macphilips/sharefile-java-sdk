package io.github.indraftapp.sharefile.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.model.ODataFeed;
import io.github.indraftapp.sharefile.core.model.WebhookSubscription;
import io.github.indraftapp.sharefile.core.odata.ODataQuery;
import org.junit.jupiter.api.Test;

class WebhookSubscriptionsClientTest {

  @Test
  void getByIdAndDeleteUseEntityEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(
        200, "{\"Id\":\"sub-1\",\"WebhookUrl\":\"https://example.test\"}");
    transport.enqueueResponse(204, new byte[0]);

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      WebhookSubscription subscription = context.webhookSubscriptionsClient().getById("sub-1");
      context.webhookSubscriptionsClient().delete("sub-1");

      assertEquals("https://example.test", subscription.getWebhookUrl());
      assertEquals(
          ClientTestSupport.BASE_URL + "/WebhookSubscriptions(sub-1)",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/WebhookSubscriptions(sub-1)",
          transport.requests.get(1).uri().toString());
      assertEquals("DELETE", transport.requests.get(1).method());
    }
  }

  @Test
  void listAndCreateUseCollectionEndpoint() {
    ClientTestSupport.TestTransport transport = new ClientTestSupport.TestTransport();
    transport.enqueueJsonResponse(200, "{\"value\":[]}");
    transport.enqueueJsonResponse(
        200, "{\"Id\":\"sub-1\",\"WebhookUrl\":\"https://example.test\"}");

    try (ClientTestSupport.TestContext context = ClientTestSupport.createContext(transport)) {
      ODataFeed<WebhookSubscription> feed =
          context.webhookSubscriptionsClient().list(ODataQuery.builder().top(5).build());

      WebhookSubscription payload = new WebhookSubscription();
      payload.setWebhookUrl("https://example.test");
      WebhookSubscription created =
          context.webhookSubscriptionsClient().create(payload, RetryPolicy.retryOnServerError(1));

      assertEquals(0, feed.getItems().size());
      assertEquals("https://example.test", created.getWebhookUrl());
      assertEquals(
          ClientTestSupport.BASE_URL + "/WebhookSubscriptions?%24top=5",
          transport.requests.get(0).uri().toString());
      assertEquals(
          ClientTestSupport.BASE_URL + "/WebhookSubscriptions",
          transport.requests.get(1).uri().toString());
      assertEquals("{\"WebhookUrl\":\"https://example.test\"}", transport.requests.get(1).body());
    }
  }
}
