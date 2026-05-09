package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SendShareRequest {

    @JsonProperty("Items")
    private List<String> items;

    @JsonProperty("Recipients")
    private List<String> recipients;

    @JsonProperty("Subject")
    private String subject;

    @JsonProperty("Body")
    private String body;

    @JsonProperty("ExpirationDate")
    private Instant expirationDate;

    @JsonProperty("RequireLogin")
    private Boolean requireLogin;

    @JsonProperty("RequireUserInfo")
    private Boolean requireUserInfo;

    @JsonProperty("IsViewOnly")
    private Boolean isViewOnly;

    @JsonProperty("MaxDownloads")
    private Integer maxDownloads;

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Boolean getRequireLogin() {
        return requireLogin;
    }

    public void setRequireLogin(Boolean requireLogin) {
        this.requireLogin = requireLogin;
    }

    public Boolean getRequireUserInfo() {
        return requireUserInfo;
    }

    public void setRequireUserInfo(Boolean requireUserInfo) {
        this.requireUserInfo = requireUserInfo;
    }

    public Boolean getIsViewOnly() {
        return isViewOnly;
    }

    public void setIsViewOnly(Boolean isViewOnly) {
        this.isViewOnly = isViewOnly;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }
}
