package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the ShareFile upload methods available for a transfer.
 */
public enum UploadMethod {

    STANDARD("Standard"),
    STREAMED("Streamed"),
    THREADED("Threaded");

    private final String value;

    UploadMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UploadMethod fromValue(String value) {
        for (UploadMethod method : values()) {
            if (method.value.equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown UploadMethod: " + value);
    }
}
