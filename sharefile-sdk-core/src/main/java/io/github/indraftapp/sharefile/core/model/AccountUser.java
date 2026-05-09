package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Represents a ShareFile user with account-level administrative capabilities. */
@Getter
@Setter
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
}
