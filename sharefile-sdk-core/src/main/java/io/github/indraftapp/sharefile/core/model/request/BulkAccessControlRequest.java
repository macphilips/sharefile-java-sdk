package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.AccessControl;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for applying access-control changes to multiple ShareFile principals.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class BulkAccessControlRequest {

    @JsonProperty("AccessControls")
    private List<AccessControl> accessControls;

    @JsonProperty("NotifyUser")
    private Boolean notifyUser;

    @JsonProperty("NotifyMessage")
    private String notifyMessage;

    @JsonProperty("Recursive")
    private Boolean recursive;
}
