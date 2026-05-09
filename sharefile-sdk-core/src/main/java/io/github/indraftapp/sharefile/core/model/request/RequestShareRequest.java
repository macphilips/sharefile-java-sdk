package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
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

  @JsonProperty("FolderID")
  private String folderID;

  @JsonProperty("TrackUntilDate")
  private Instant trackUntilDate;

  @JsonProperty("SendFrequency")
  private Integer sendFrequency;

  @JsonProperty("SendInterval")
  private Integer sendInterval;
}
