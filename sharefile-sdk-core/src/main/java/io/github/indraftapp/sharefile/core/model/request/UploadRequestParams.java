package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.enums.UploadMethod;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadRequestParams {

    @JsonProperty("Method")
    private UploadMethod method;

    @JsonProperty("FileName")
    private String fileName;

    @JsonProperty("FileSize")
    private Long fileSize;

    @JsonProperty("Details")
    private String details;

    @JsonProperty("IsSend")
    private Boolean isSend;

    @JsonProperty("ThreadCount")
    private Integer threadCount;

    @JsonProperty("Overwrite")
    private Boolean overwrite;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Tool")
    private String tool;

    @JsonProperty("Raw")
    private Boolean raw;

    @JsonProperty("BatchId")
    private String batchId;

    public UploadMethod getMethod() {
        return method;
    }

    public void setMethod(UploadMethod method) {
        this.method = method;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Boolean getIsSend() {
        return isSend;
    }

    public void setIsSend(Boolean isSend) {
        this.isSend = isSend;
    }

    public Integer getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public Boolean getRaw() {
        return raw;
    }

    public void setRaw(Boolean raw) {
        this.raw = raw;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
}
