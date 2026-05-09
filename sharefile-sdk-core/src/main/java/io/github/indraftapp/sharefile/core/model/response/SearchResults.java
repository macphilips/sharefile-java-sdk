package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.Item;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Response payload for basic ShareFile search results. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SearchResults {

  @JsonProperty("Results")
  private List<Item> results;

  @JsonProperty("TotalCount")
  private Integer totalCount;

  @JsonProperty("TimedOut")
  private Boolean timedOut;
}
