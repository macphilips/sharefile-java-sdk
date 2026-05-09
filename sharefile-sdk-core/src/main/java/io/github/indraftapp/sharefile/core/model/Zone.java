package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.enums.ZoneService;

public class Zone extends ODataEntity {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("HeartbeatTolerance")
    private Integer heartbeatTolerance;

    @JsonProperty("ZoneServices")
    private ZoneService zoneServices;

    @JsonProperty("Secret")
    private String secret;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHeartbeatTolerance() {
        return heartbeatTolerance;
    }

    public void setHeartbeatTolerance(Integer heartbeatTolerance) {
        this.heartbeatTolerance = heartbeatTolerance;
    }

    public ZoneService getZoneServices() {
        return zoneServices;
    }

    public void setZoneServices(ZoneService zoneServices) {
        this.zoneServices = zoneServices;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
