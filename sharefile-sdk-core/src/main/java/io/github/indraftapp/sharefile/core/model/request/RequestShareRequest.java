package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestShareRequest {

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

    @JsonProperty("FolderID")
    private String folderID;

    @JsonProperty("TrackUntilDate")
    private Instant trackUntilDate;

    @JsonProperty("SendFrequency")
    private Integer sendFrequency;

    @JsonProperty("SendInterval")
    private Integer sendInterval;

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

    public String getFolderID() {
        return folderID;
    }

    public void setFolderID(String folderID) {
        this.folderID = folderID;
    }

    public Instant getTrackUntilDate() {
        return trackUntilDate;
    }

    public void setTrackUntilDate(Instant trackUntilDate) {
        this.trackUntilDate = trackUntilDate;
    }

    public Integer getSendFrequency() {
        return sendFrequency;
    }

    public void setSendFrequency(Integer sendFrequency) {
        this.sendFrequency = sendFrequency;
    }

    public Integer getSendInterval() {
        return sendInterval;
    }

    public void setSendInterval(Integer sendInterval) {
        this.sendInterval = sendInterval;
    }
}
