package io.github.indraftapp.sharefile.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.indraftapp.sharefile.core.model.Item;
import io.github.indraftapp.sharefile.core.model.OAuthToken;
import io.github.indraftapp.sharefile.core.model.Session;
import io.github.indraftapp.sharefile.core.model.WebhookSubscription;
import org.junit.jupiter.api.Test;

class ShareFileFixturesTest {

  @Test
  void readsFixtureAsStringBytesAndJsonNode() {
    String json = ShareFileFixtures.readString("items/get-by-id.json");
    byte[] bytes = ShareFileFixtures.readBytes("items/get-by-id.json");
    JsonNode node = ShareFileFixtures.readJsonNode("items/get-by-id.json");

    assertThat(json).contains("\"Name\": \"My Document.pdf\"");
    assertThat(bytes).isNotEmpty();
    assertThat(node.path("Id").asText()).isEqualTo("item-123");
  }

  @Test
  void deserializesRepresentativeFixtureModels() {
    Item item = ShareFileFixtures.readValue("items/get-by-id.json", Item.class);
    WebhookSubscription subscription =
        ShareFileFixtures.readValue(
            "webhook-subscriptions/get-by-id.json", WebhookSubscription.class);
    Session session = ShareFileFixtures.readValue("sessions/get.json", Session.class);
    OAuthToken token = ShareFileFixtures.readValue("auth/token.json", OAuthToken.class);

    assertThat(item.getId()).isEqualTo("item-123");
    assertThat(subscription.getEvents()).hasSize(1);
    assertThat(subscription.getEvents().get(0).getOperationName()).isEqualTo("Upload");
    assertThat(session.getId()).isEqualTo("session-123");
    assertThat(token.getAccessToken()).isEqualTo("access-token");
  }
}
