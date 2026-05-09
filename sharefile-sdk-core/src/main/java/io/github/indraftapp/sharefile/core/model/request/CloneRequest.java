package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloneRequest {

    @JsonProperty("TargetItemId")
    private String targetItemId;

    @JsonProperty("Overwrite")
    private Boolean overwrite;

    public String getTargetItemId() {
        return targetItemId;
    }

    public void setTargetItemId(String targetItemId) {
        this.targetItemId = targetItemId;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }
}
