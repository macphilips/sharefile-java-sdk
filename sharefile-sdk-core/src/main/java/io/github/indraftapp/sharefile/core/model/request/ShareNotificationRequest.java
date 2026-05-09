package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request payload for sending notifications about an existing ShareFile share.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareNotificationRequest {

    @JsonProperty("Recipients")
    private List<String> recipients;

    @JsonProperty("Subject")
    private String subject;

    @JsonProperty("Body")
    private String body;

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
