package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.Item;

import java.util.List;

/**
 * Response payload for ShareFile advanced-search results.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdvancedSearchResults {

    @JsonProperty("Results")
    private List<Item> results;

    @JsonProperty("TotalCount")
    private Integer totalCount;

    @JsonProperty("PartialResults")
    private Boolean partialResults;

    @JsonProperty("TimedOut")
    private Boolean timedOut;

    public List<Item> getResults() {
        return results;
    }

    public void setResults(List<Item> results) {
        this.results = results;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Boolean getPartialResults() {
        return partialResults;
    }

    public void setPartialResults(Boolean partialResults) {
        this.partialResults = partialResults;
    }

    public Boolean getTimedOut() {
        return timedOut;
    }

    public void setTimedOut(Boolean timedOut) {
        this.timedOut = timedOut;
    }
}
