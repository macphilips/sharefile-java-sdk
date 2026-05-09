package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Request payload for deleting multiple ShareFile items. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class BulkDeleteRequest {

  @JsonProperty("ItemIds")
  private List<String> itemIds;

  @JsonProperty("ForceSync")
  private Boolean forceSync;
}
