package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents access-control permissions granted to a ShareFile principal for an item.
 */
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

    public ODataEntity getPrincipal() {
        return principal;
    }

    public void setPrincipal(ODataEntity principal) {
        this.principal = principal;
    }

    public Boolean getCanUpload() {
        return canUpload;
    }

    public void setCanUpload(Boolean canUpload) {
        this.canUpload = canUpload;
    }

    public Boolean getCanDownload() {
        return canDownload;
    }

    public void setCanDownload(Boolean canDownload) {
        this.canDownload = canDownload;
    }

    public Boolean getCanView() {
        return canView;
    }

    public void setCanView(Boolean canView) {
        this.canView = canView;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    public Boolean getCanManagePermissions() {
        return canManagePermissions;
    }

    public void setCanManagePermissions(Boolean canManagePermissions) {
        this.canManagePermissions = canManagePermissions;
    }
}
