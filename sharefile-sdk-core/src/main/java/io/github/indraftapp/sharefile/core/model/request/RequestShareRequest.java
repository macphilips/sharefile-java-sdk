package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** Request payload for creating a ShareFile request-a-file share. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
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

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String folderID;

  @JsonProperty("TrackUntilDate")
  private Instant trackUntilDate;

  @JsonProperty("SendFrequency")
  private Integer sendFrequency;

  @JsonProperty("SendInterval")
  private Integer sendInterval;

  @JsonProperty("ShareType")
  public String getShareType() {
    return "Request";
  }

  @JsonProperty("Parent")
  public ParentReference getParent() {
    if (folderID == null || folderID.isBlank()) {
      return null;
    }
    return new ParentReference(folderID);
  }

  @JsonIgnore
  public String getFolderID() {
    return folderID;
  }

  public void setFolderID(String folderID) {
    this.folderID = folderID;
  }

  /** Minimal parent reference shape required by request-share creation. */
  public record ParentReference(@JsonProperty("Id") String id) {}
}
