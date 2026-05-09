package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.response.SubscriptionContext;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Represents a ShareFile webhook subscription. */
@Getter
@Setter
public class WebhookSubscription extends ODataEntity {

  @JsonProperty("SubscriptionContext")
  private SubscriptionContext subscriptionContext;

  @JsonProperty("WebhookUrl")
  private String webhookUrl;

  @JsonProperty("Events")
  private List<String> events;
}
