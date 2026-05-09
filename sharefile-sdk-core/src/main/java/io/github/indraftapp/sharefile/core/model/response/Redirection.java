package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Response payload describing a redirect target for an item resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Redirection {

    @JsonProperty("Uri")
    private String uri;

    @JsonProperty("Available")
    private Boolean available;

    @JsonProperty("Expiration")
    private Instant expiration;
}
