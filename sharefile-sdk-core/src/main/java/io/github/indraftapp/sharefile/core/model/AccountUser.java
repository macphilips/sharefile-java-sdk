package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountUser extends User {

    @JsonProperty("StorageQuotaLimitGB")
    private Integer storageQuotaLimitGB;

    @JsonProperty("IsAdministrator")
    private Boolean isAdministrator;

    @JsonProperty("CanCreateFolders")
    private Boolean canCreateFolders;

    @JsonProperty("CanUseFileBox")
    private Boolean canUseFileBox;

    @JsonProperty("CanManageUsers")
    private Boolean canManageUsers;

    @JsonProperty("Bandwidth")
    private Integer bandwidth;

    public Integer getStorageQuotaLimitGB() {
        return storageQuotaLimitGB;
    }

    public void setStorageQuotaLimitGB(Integer storageQuotaLimitGB) {
        this.storageQuotaLimitGB = storageQuotaLimitGB;
    }

    public Boolean getIsAdministrator() {
        return isAdministrator;
    }

    public void setIsAdministrator(Boolean isAdministrator) {
        this.isAdministrator = isAdministrator;
    }

    public Boolean getCanCreateFolders() {
        return canCreateFolders;
    }

    public void setCanCreateFolders(Boolean canCreateFolders) {
        this.canCreateFolders = canCreateFolders;
    }

    public Boolean getCanUseFileBox() {
        return canUseFileBox;
    }

    public void setCanUseFileBox(Boolean canUseFileBox) {
        this.canUseFileBox = canUseFileBox;
    }

    public Boolean getCanManageUsers() {
        return canManageUsers;
    }

    public void setCanManageUsers(Boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }

    public Integer getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Integer bandwidth) {
        this.bandwidth = bandwidth;
    }
}
