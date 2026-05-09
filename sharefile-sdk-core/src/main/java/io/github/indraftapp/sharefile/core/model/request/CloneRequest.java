package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Request payload for cloning a ShareFile item into another location. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class CloneRequest {

  @JsonProperty("TargetItemId")
  private String targetItemId;

  @JsonProperty("Overwrite")
  private Boolean overwrite;
}
