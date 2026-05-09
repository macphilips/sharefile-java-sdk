package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PreviewStatus {

    NONE("None"),
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable");

    private final String value;

    PreviewStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PreviewStatus fromValue(String value) {
        for (PreviewStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PreviewStatus: " + value);
    }
}
