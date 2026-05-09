package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for sending notifications about an existing ShareFile share.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ShareNotificationRequest {

    @JsonProperty("Recipients")
    private List<String> recipients;

    @JsonProperty("Subject")
    private String subject;

    @JsonProperty("Body")
    private String body;
}
