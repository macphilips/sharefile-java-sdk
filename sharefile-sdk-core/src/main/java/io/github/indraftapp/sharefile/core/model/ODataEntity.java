package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base type for ShareFile OData entities that expose standard metadata fields.
 */
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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
