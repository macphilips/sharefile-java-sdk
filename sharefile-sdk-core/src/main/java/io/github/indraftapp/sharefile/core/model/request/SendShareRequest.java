package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** Request payload for creating and sending a ShareFile share. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SendShareRequest {

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private List<String> items;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
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

  @JsonProperty("IsViewOnly")
  private Boolean isViewOnly;

  @JsonProperty("MaxDownloads")
  private Integer maxDownloads;

  @JsonProperty("ShareType")
  public String getShareType() {
    return "Send";
  }

  @JsonProperty("Items")
  public List<ItemReference> getSerializedItems() {
    if (items == null) {
      return null;
    }
    return items.stream().map(ItemReference::new).toList();
  }

  @JsonProperty("Recipients")
  public List<ShareRecipient> getSerializedRecipients() {
    if (recipients == null) {
      return null;
    }
    return recipients.stream().map(ShareRecipient::fromEmail).toList();
  }

  @JsonIgnore
  public List<String> getItems() {
    return items;
  }

  public void setItems(List<String> items) {
    this.items = items;
  }

  @JsonIgnore
  public List<String> getRecipients() {
    return recipients;
  }

  public void setRecipients(List<String> recipients) {
    this.recipients = recipients;
  }

  /** Minimal item reference shape required by the send-share API. */
  public record ItemReference(@JsonProperty("Id") String id) {}
}
