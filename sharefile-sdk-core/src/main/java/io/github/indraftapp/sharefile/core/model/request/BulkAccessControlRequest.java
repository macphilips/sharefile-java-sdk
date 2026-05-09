package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.AccessControl;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkAccessControlRequest {

    @JsonProperty("AccessControls")
    private List<AccessControl> accessControls;

    @JsonProperty("NotifyUser")
    private Boolean notifyUser;

    @JsonProperty("NotifyMessage")
    private String notifyMessage;

    @JsonProperty("Recursive")
    private Boolean recursive;

    public List<AccessControl> getAccessControls() {
        return accessControls;
    }

    public void setAccessControls(List<AccessControl> accessControls) {
        this.accessControls = accessControls;
    }

    public Boolean getNotifyUser() {
        return notifyUser;
    }

    public void setNotifyUser(Boolean notifyUser) {
        this.notifyUser = notifyUser;
    }

    public String getNotifyMessage() {
        return notifyMessage;
    }

    public void setNotifyMessage(String notifyMessage) {
        this.notifyMessage = notifyMessage;
    }

    public Boolean getRecursive() {
        return recursive;
    }

    public void setRecursive(Boolean recursive) {
        this.recursive = recursive;
    }
}
