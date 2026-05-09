package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload containing folder and root-location metadata for an item.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemInfo {

    @JsonProperty("HasVroot")
    private Boolean hasVroot;

    @JsonProperty("IsSystemRoot")
    private Boolean isSystemRoot;

    @JsonProperty("IsAccountRoot")
    private Boolean isAccountRoot;

    @JsonProperty("IsVRoot")
    private Boolean isVRoot;

    @JsonProperty("IsMyFolders")
    private Boolean isMyFolders;

    @JsonProperty("IsAHomeFolder")
    private Boolean isAHomeFolder;

    @JsonProperty("IsMyHomeFolder")
    private Boolean isMyHomeFolder;

    @JsonProperty("IsAStartFolder")
    private Boolean isAStartFolder;

    @JsonProperty("IsSharedFolder")
    private Boolean isSharedFolder;

    @JsonProperty("IsPassthrough")
    private Boolean isPassthrough;

    @JsonProperty("CanAddFolder")
    private Boolean canAddFolder;

    @JsonProperty("CanAddNode")
    private Boolean canAddNode;

    @JsonProperty("CanView")
    private Boolean canView;

    @JsonProperty("CanDownload")
    private Boolean canDownload;

    @JsonProperty("CanUpload")
    private Boolean canUpload;

    @JsonProperty("CanSend")
    private Boolean canSend;

    @JsonProperty("CanDeleteCurrentItem")
    private Boolean canDeleteCurrentItem;

    @JsonProperty("CanDeleteChildItems")
    private Boolean canDeleteChildItems;

    @JsonProperty("CanManagePermissions")
    private Boolean canManagePermissions;

    @JsonProperty("FolderPayID")
    private String folderPayID;

    @JsonProperty("ShowFolderPayBuyButton")
    private Boolean showFolderPayBuyButton;

    public Boolean getHasVroot() {
        return hasVroot;
    }

    public void setHasVroot(Boolean hasVroot) {
        this.hasVroot = hasVroot;
    }

    public Boolean getIsSystemRoot() {
        return isSystemRoot;
    }

    public void setIsSystemRoot(Boolean isSystemRoot) {
        this.isSystemRoot = isSystemRoot;
    }

    public Boolean getIsAccountRoot() {
        return isAccountRoot;
    }

    public void setIsAccountRoot(Boolean isAccountRoot) {
        this.isAccountRoot = isAccountRoot;
    }

    public Boolean getIsVRoot() {
        return isVRoot;
    }

    public void setIsVRoot(Boolean isVRoot) {
        this.isVRoot = isVRoot;
    }

    public Boolean getIsMyFolders() {
        return isMyFolders;
    }

    public void setIsMyFolders(Boolean isMyFolders) {
        this.isMyFolders = isMyFolders;
    }

    public Boolean getIsAHomeFolder() {
        return isAHomeFolder;
    }

    public void setIsAHomeFolder(Boolean isAHomeFolder) {
        this.isAHomeFolder = isAHomeFolder;
    }

    public Boolean getIsMyHomeFolder() {
        return isMyHomeFolder;
    }

    public void setIsMyHomeFolder(Boolean isMyHomeFolder) {
        this.isMyHomeFolder = isMyHomeFolder;
    }

    public Boolean getIsAStartFolder() {
        return isAStartFolder;
    }

    public void setIsAStartFolder(Boolean isAStartFolder) {
        this.isAStartFolder = isAStartFolder;
    }

    public Boolean getIsSharedFolder() {
        return isSharedFolder;
    }

    public void setIsSharedFolder(Boolean isSharedFolder) {
        this.isSharedFolder = isSharedFolder;
    }

    public Boolean getIsPassthrough() {
        return isPassthrough;
    }

    public void setIsPassthrough(Boolean isPassthrough) {
        this.isPassthrough = isPassthrough;
    }

    public Boolean getCanAddFolder() {
        return canAddFolder;
    }

    public void setCanAddFolder(Boolean canAddFolder) {
        this.canAddFolder = canAddFolder;
    }

    public Boolean getCanAddNode() {
        return canAddNode;
    }

    public void setCanAddNode(Boolean canAddNode) {
        this.canAddNode = canAddNode;
    }

    public Boolean getCanView() {
        return canView;
    }

    public void setCanView(Boolean canView) {
        this.canView = canView;
    }

    public Boolean getCanDownload() {
        return canDownload;
    }

    public void setCanDownload(Boolean canDownload) {
        this.canDownload = canDownload;
    }

    public Boolean getCanUpload() {
        return canUpload;
    }

    public void setCanUpload(Boolean canUpload) {
        this.canUpload = canUpload;
    }

    public Boolean getCanSend() {
        return canSend;
    }

    public void setCanSend(Boolean canSend) {
        this.canSend = canSend;
    }

    public Boolean getCanDeleteCurrentItem() {
        return canDeleteCurrentItem;
    }

    public void setCanDeleteCurrentItem(Boolean canDeleteCurrentItem) {
        this.canDeleteCurrentItem = canDeleteCurrentItem;
    }

    public Boolean getCanDeleteChildItems() {
        return canDeleteChildItems;
    }

    public void setCanDeleteChildItems(Boolean canDeleteChildItems) {
        this.canDeleteChildItems = canDeleteChildItems;
    }

    public Boolean getCanManagePermissions() {
        return canManagePermissions;
    }

    public void setCanManagePermissions(Boolean canManagePermissions) {
        this.canManagePermissions = canManagePermissions;
    }

    public String getFolderPayID() {
        return folderPayID;
    }

    public void setFolderPayID(String folderPayID) {
        this.folderPayID = folderPayID;
    }

    public Boolean getShowFolderPayBuyButton() {
        return showFolderPayBuyButton;
    }

    public void setShowFolderPayBuyButton(Boolean showFolderPayBuyButton) {
        this.showFolderPayBuyButton = showFolderPayBuyButton;
    }
}
