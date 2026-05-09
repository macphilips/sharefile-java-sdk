package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload containing ShareFile download URLs and preparation state.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DownloadSpecification {

    @JsonProperty("DownloadUrl")
    private String downloadUrl;

    @JsonProperty("DownloadToken")
    private String downloadToken;

    @JsonProperty("PrepStatus")
    private String prepStatus;

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadToken() {
        return downloadToken;
    }

    public void setDownloadToken(String downloadToken) {
        this.downloadToken = downloadToken;
    }

    public String getPrepStatus() {
        return prepStatus;
    }

    public void setPrepStatus(String prepStatus) {
        this.prepStatus = prepStatus;
    }
}
