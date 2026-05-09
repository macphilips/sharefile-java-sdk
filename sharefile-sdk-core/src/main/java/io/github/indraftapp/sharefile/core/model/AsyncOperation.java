package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
     * Returns true if the operation is in a terminal state (Completed, Error, or Cancelled).
     */
    public boolean isTerminal() {
        return "Completed".equals(state) || "Error".equals(state) || "Cancelled".equals(state);
    }
}
