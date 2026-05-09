package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.enums.UploadMethod;

/**
 * Response payload containing ShareFile upload endpoints and resume metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadSpecification {

    @JsonProperty("Method")
    private UploadMethod method;

    @JsonProperty("ChunkUri")
    private String chunkUri;

    @JsonProperty("IsResume")
    private Boolean isResume;

    @JsonProperty("ResumeIndex")
    private Long resumeIndex;

    @JsonProperty("ResumeOffset")
    private Long resumeOffset;

    @JsonProperty("ResumeFileHash")
    private String resumeFileHash;

    @JsonProperty("FinishUri")
    private String finishUri;

    public UploadMethod getMethod() {
        return method;
    }

    public void setMethod(UploadMethod method) {
        this.method = method;
    }

    public String getChunkUri() {
        return chunkUri;
    }

    public void setChunkUri(String chunkUri) {
        this.chunkUri = chunkUri;
    }

    public Boolean getIsResume() {
        return isResume;
    }

    public void setIsResume(Boolean isResume) {
        this.isResume = isResume;
    }

    public Long getResumeIndex() {
        return resumeIndex;
    }

    public void setResumeIndex(Long resumeIndex) {
        this.resumeIndex = resumeIndex;
    }

    public Long getResumeOffset() {
        return resumeOffset;
    }

    public void setResumeOffset(Long resumeOffset) {
        this.resumeOffset = resumeOffset;
    }

    public String getResumeFileHash() {
        return resumeFileHash;
    }

    public void setResumeFileHash(String resumeFileHash) {
        this.resumeFileHash = resumeFileHash;
    }

    public String getFinishUri() {
        return finishUri;
    }

    public void setFinishUri(String finishUri) {
        this.finishUri = finishUri;
    }
}
