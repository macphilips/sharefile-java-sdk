package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for creating a ShareFile folder.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderCreateRequest {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("OverWrite")
    private Boolean overWrite;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getOverWrite() {
        return overWrite;
    }

    public void setOverWrite(Boolean overWrite) {
        this.overWrite = overWrite;
    }
}
