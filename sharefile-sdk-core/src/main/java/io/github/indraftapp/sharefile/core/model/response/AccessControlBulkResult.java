package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response payload describing the outcome of a bulk access-control operation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class AccessControlBulkResult {

    @JsonProperty("Succeeded")
    private Integer succeeded;

    @JsonProperty("Failed")
    private Integer failed;

    @JsonProperty("Errors")
    private List<String> errors;
}
