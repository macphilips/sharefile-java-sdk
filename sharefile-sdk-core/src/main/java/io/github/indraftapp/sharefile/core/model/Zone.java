package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.enums.ZoneService;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a ShareFile storage zone.
 */
@Getter
@Setter
public class Zone extends ODataEntity {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("HeartbeatTolerance")
    private Integer heartbeatTolerance;

    @JsonProperty("ZoneServices")
    private ZoneService zoneServices;

    @JsonProperty("Secret")
    private String secret;
}
