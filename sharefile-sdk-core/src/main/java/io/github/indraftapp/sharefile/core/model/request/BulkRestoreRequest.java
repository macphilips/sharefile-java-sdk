package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for restoring multiple ShareFile items.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class BulkRestoreRequest {

    @JsonProperty("ItemIds")
    private List<String> itemIds;
}
