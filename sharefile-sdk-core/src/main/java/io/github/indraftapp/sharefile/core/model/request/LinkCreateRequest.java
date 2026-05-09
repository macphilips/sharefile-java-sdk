package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for creating a ShareFile link item.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkCreateRequest {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Uri")
    private String uri;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
