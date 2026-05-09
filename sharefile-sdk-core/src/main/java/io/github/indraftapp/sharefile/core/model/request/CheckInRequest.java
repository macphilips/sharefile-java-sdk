package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for checking a file back into ShareFile.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class CheckInRequest {

    @JsonProperty("Comment")
    private String comment;
}
