package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents ShareFile item ordering modes.
 */
public enum ItemOrderingMode {

    FOLDERS_FIRST("FoldersFirst"),
    DATE_DESC("DateDesc"),
    DATE_ASC("DateAsc"),
    NAME_ASC("NameAsc"),
    NAME_DESC("NameDesc");

    private final String value;

    ItemOrderingMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ItemOrderingMode fromValue(String value) {
        for (ItemOrderingMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown ItemOrderingMode: " + value);
    }
}
