package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the service types exposed by a ShareFile storage zone.
 */
public enum ZoneService {

    STORAGE_ZONE("StorageZone"),
    SHARE_POINT("SharePoint"),
    NETWORK_SHARE_CONNECTOR("NetworkShareConnector");

    private final String value;

    ZoneService(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ZoneService fromValue(String value) {
        for (ZoneService service : values()) {
            if (service.value.equalsIgnoreCase(value)) {
                return service;
            }
        }
        throw new IllegalArgumentException("Unknown ZoneService: " + value);
    }
}
