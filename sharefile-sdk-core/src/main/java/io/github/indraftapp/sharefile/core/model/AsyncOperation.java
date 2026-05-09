package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a server-side ShareFile asynchronous operation.
 */
public class AsyncOperation extends ODataEntity {

    @JsonProperty("State")
    private String state;

    @JsonProperty("BatchId")
    private String batchId;

    @JsonProperty("Progress")
    private Integer progress;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    /**
     * Returns {@code true} when the operation has reached a terminal state and no further polling is required.
     *
     * @return {@code true} when the operation state is terminal
     */
    public boolean isTerminal() {
        return "Completed".equals(state) || "Error".equals(state) || "Cancelled".equals(state);
    }
}
