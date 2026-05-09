package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for ShareFile advanced-search operations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdvancedSearchRequest {

    @JsonProperty("Query")
    private String query;

    @JsonProperty("ItemType")
    private String itemType;

    @JsonProperty("ParentID")
    private String parentID;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getParentID() {
        return parentID;
    }

    public void setParentID(String parentID) {
        this.parentID = parentID;
    }
}
