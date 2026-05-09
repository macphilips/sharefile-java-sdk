package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataFeed<T> implements Iterable<T> {

    @JsonProperty("odata.metadata")
    private String metadata;

    @JsonProperty("odata.count")
    private Integer count;

    @JsonProperty("odata.nextLink")
    private String nextLink;

    @JsonProperty("value")
    private List<T> value;

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getNextLink() {
        return nextLink;
    }

    public void setNextLink(String nextLink) {
        this.nextLink = nextLink;
    }

    public List<T> getValue() {
        return value;
    }

    public void setValue(List<T> value) {
        this.value = value;
    }

    public boolean hasNextPage() {
        return nextLink != null && !nextLink.isEmpty();
    }

    public List<T> getItems() {
        return value != null ? value : Collections.emptyList();
    }

    @Override
    public Iterator<T> iterator() {
        return getItems().iterator();
    }
}
