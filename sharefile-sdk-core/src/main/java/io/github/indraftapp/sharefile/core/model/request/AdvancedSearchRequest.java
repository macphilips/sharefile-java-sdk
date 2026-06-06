package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Request payload for ShareFile advanced-search operations. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class AdvancedSearchRequest {

  @JsonProperty("Query")
  private String query;

  @JsonProperty("ItemType")
  private String itemType;

  @JsonProperty("ParentID")
  private String parentID;
}
