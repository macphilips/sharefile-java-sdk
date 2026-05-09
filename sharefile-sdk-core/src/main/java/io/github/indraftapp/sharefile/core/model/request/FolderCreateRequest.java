package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Request payload for creating a ShareFile folder. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class FolderCreateRequest {

  @JsonProperty("Name")
  private String name;

  @JsonProperty("Description")
  private String description;

  @JsonProperty("OverWrite")
  private Boolean overWrite;
}
