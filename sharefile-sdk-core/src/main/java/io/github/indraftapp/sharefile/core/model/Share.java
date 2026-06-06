package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.enums.ShareType;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Represents a ShareFile share and its sharing configuration. */
@Getter
@Setter
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
}
