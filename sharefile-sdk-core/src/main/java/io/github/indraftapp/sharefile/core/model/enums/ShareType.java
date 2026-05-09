package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Represents the supported ShareFile share types. */
public enum ShareType {
  SEND("Send"),
  REQUEST("Request");

  private final String value;

  ShareType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ShareType fromValue(String value) {
    for (ShareType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown ShareType: " + value);
  }
}
