package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Response payload describing a redirect target for an item resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Redirection {

    @JsonProperty("Uri")
    private String uri;

    @JsonProperty("Available")
    private Boolean available;

    @JsonProperty("Expiration")
    private Instant expiration;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }
}
