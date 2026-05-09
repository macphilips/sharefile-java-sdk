package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Request payload for creating and sending a ShareFile share.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SendShareRequest {

    @JsonProperty("Items")
    private List<String> items;

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

    @JsonProperty("IsViewOnly")
    private Boolean isViewOnly;

    @JsonProperty("MaxDownloads")
    private Integer maxDownloads;
}
