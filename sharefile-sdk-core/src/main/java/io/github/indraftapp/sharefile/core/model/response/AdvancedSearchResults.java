package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.Item;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response payload for ShareFile advanced-search results.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class AdvancedSearchResults {

    @JsonProperty("Results")
    private List<Item> results;

    @JsonProperty("TotalCount")
    private Integer totalCount;

    @JsonProperty("PartialResults")
    private Boolean partialResults;

    @JsonProperty("TimedOut")
    private Boolean timedOut;
}
