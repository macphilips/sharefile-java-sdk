package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload describing the status of an uploaded chunk.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkResult {

    @JsonProperty("ChunkNumber")
    private Integer chunkNumber;

    @JsonProperty("IsComplete")
    private Boolean isComplete;

    @JsonProperty("Hash")
    private String hash;

    public Integer getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(Integer chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean isComplete) {
        this.isComplete = isComplete;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
