package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload describing the final result of a ShareFile upload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadResult {

    @JsonProperty("ItemId")
    private String itemId;

    @JsonProperty("FileName")
    private String fileName;

    @JsonProperty("FileSize")
    private Long fileSize;

    @JsonProperty("Details")
    private String details;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
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
}
