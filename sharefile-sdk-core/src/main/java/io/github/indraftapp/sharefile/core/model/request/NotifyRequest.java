package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for notifying ShareFile users with a custom message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class NotifyRequest {

    @JsonProperty("UserIds")
    private List<String> userIds;

    @JsonProperty("Message")
    private String message;
}
