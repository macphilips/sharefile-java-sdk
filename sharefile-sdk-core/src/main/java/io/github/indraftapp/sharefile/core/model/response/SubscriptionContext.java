package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Response payload describing the resource context for a webhook subscription.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SubscriptionContext {

    @JsonProperty("ResourceType")
    private String resourceType;

    @JsonProperty("ResourceId")
    private String resourceId;
}
