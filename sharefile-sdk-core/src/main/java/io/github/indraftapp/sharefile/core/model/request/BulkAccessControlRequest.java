package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.AccessControl;
import java.util.List;

/** Request payload for applying access-control changes to multiple ShareFile principals. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkAccessControlRequest {

  private List<AccessControl> accessControls;
  private Boolean notifyUser;
  private String notifyMessage;
  private Boolean recursive;
  private List<AccessControlParam> accessControlParams;

  @JsonProperty("AccessControlParams")
  public List<AccessControlParam> getSerializedAccessControlParams() {
    if (accessControlParams != null) {
      return accessControlParams;
    }
    if (accessControls == null) {
      return null;
    }
    return accessControls.stream()
        .map(accessControl -> new AccessControlParam(accessControl, notifyUser, recursive))
        .toList();
  }

  public void setAccessControlParams(List<AccessControlParam> accessControlParams) {
    this.accessControlParams = accessControlParams;
  }

  @JsonIgnore
  public List<AccessControlParam> getAccessControlParams() {
    return accessControlParams;
  }

  @JsonIgnore
  public List<AccessControl> getAccessControls() {
    return accessControls;
  }

  public void setAccessControls(List<AccessControl> accessControls) {
    this.accessControls = accessControls;
  }

  @JsonIgnore
  public Boolean getNotifyUser() {
    return notifyUser;
  }

  public void setNotifyUser(Boolean notifyUser) {
    this.notifyUser = notifyUser;
  }

  @JsonProperty("NotifyMessage")
  public String getNotifyMessage() {
    return notifyMessage;
  }

  public void setNotifyMessage(String notifyMessage) {
    this.notifyMessage = notifyMessage;
  }

  @JsonIgnore
  public Boolean getRecursive() {
    return recursive;
  }

  public void setRecursive(Boolean recursive) {
    this.recursive = recursive;
  }

  /** Per-entry access-control payload used by BulkSet endpoints. */
  public record AccessControlParam(
      @JsonProperty("AccessControl") AccessControl accessControl,
      @JsonProperty("NotifyUser") Boolean notifyUser,
      @JsonProperty("Recursive") Boolean recursive) {}
}
