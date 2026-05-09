package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Represents the OAuth grant types supported by the SDK. */
public enum GrantType {
  AUTHORIZATION_CODE("authorization_code"),
  PASSWORD("password"),
  REFRESH_TOKEN("refresh_token");

  private final String value;

  GrantType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static GrantType fromValue(String value) {
    for (GrantType type : values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown GrantType: " + value);
  }
}
