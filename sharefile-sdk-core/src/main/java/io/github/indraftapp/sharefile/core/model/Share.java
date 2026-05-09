package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.enums.ShareType;

import java.time.Instant;
import java.util.List;

/**
 * Represents a ShareFile share and its sharing configuration.
 */
public class Share extends ODataEntity {

    @JsonProperty("ShareType")
    private ShareType shareType;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Items")
    private List<Item> items;

    @JsonProperty("Recipients")
    private List<ShareAlias> recipients;

    @JsonProperty("Parent")
    private Item parent;

    @JsonProperty("ExpirationDate")
    private Instant expirationDate;

    @JsonProperty("RequireLogin")
    private Boolean requireLogin;

    @JsonProperty("RequireUserInfo")
    private Boolean requireUserInfo;

    @JsonProperty("IsViewOnly")
    private Boolean isViewOnly;

    @JsonProperty("TrackUntilDate")
    private Instant trackUntilDate;

    @JsonProperty("SendFrequency")
    private Integer sendFrequency;

    @JsonProperty("SendInterval")
    private Integer sendInterval;

    @JsonProperty("AliasID")
    private String aliasID;

    public ShareType getShareType() {
        return shareType;
    }

    public void setShareType(ShareType shareType) {
        this.shareType = shareType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<ShareAlias> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<ShareAlias> recipients) {
        this.recipients = recipients;
    }

    public Item getParent() {
        return parent;
    }

    public void setParent(Item parent) {
        this.parent = parent;
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

    public String getAliasID() {
        return aliasID;
    }

    public void setAliasID(String aliasID) {
        this.aliasID = aliasID;
    }
}
