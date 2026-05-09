package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Base type for ShareFile OData entities that expose standard metadata fields. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ODataEntity {

  @JsonProperty("odata.metadata")
  private String metadata;

  @JsonProperty("odata.type")
  private String type;

  @JsonProperty("Id")
  private String id;

  @JsonProperty("url")
  private String url;
}
