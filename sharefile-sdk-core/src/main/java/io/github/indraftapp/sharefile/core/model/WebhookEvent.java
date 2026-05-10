package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Represents a webhook event subscription entry. */
@Getter
@Setter
public class WebhookEvent {

  @JsonProperty("ResourceType")
  private String resourceType;

  @JsonProperty("OperationName")
  private String operationName;
}
