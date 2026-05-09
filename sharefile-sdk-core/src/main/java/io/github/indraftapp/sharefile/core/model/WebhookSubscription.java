package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.response.SubscriptionContext;

import java.util.List;

/**
 * Represents a ShareFile webhook subscription.
 */
public class WebhookSubscription extends ODataEntity {

    @JsonProperty("SubscriptionContext")
    private SubscriptionContext subscriptionContext;

    @JsonProperty("WebhookUrl")
    private String webhookUrl;

    @JsonProperty("Events")
    private List<String> events;

    public SubscriptionContext getSubscriptionContext() {
        return subscriptionContext;
    }

    public void setSubscriptionContext(SubscriptionContext subscriptionContext) {
        this.subscriptionContext = subscriptionContext;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }
}
