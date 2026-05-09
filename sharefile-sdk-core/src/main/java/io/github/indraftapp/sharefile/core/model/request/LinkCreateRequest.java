package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating a ShareFile link item.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class LinkCreateRequest {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Uri")
    private String uri;
}
