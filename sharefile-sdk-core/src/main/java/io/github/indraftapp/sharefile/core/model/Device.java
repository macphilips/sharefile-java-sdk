package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a device known to a ShareFile account.
 */
public class Device extends ODataEntity {

    @JsonProperty("DeviceName")
    private String deviceName;

    @JsonProperty("DeviceType")
    private String deviceType;

    @JsonProperty("User")
    private User user;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
