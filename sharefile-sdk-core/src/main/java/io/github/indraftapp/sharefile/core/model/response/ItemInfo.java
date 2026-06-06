package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Response payload containing folder and root-location metadata for an item. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
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
}
