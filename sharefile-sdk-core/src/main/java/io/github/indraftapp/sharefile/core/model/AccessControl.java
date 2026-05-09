package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Represents access-control permissions granted to a ShareFile principal for an item. */
@Getter
@Setter
public class AccessControl extends ODataEntity {

  @JsonProperty("Principal")
  private ODataEntity principal;

  @JsonProperty("CanUpload")
  private Boolean canUpload;

  @JsonProperty("CanDownload")
  private Boolean canDownload;

  @JsonProperty("CanView")
  private Boolean canView;

  @JsonProperty("CanDelete")
  private Boolean canDelete;

  @JsonProperty("CanManagePermissions")
  private Boolean canManagePermissions;
}
