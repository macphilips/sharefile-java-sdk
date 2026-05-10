package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.extern.slf4j.Slf4j;

/** Represents the availability state of an item preview. */
@Slf4j
public enum PreviewStatus {
  NONE("None"),
  AVAILABLE("Available"),
  UNAVAILABLE("Unavailable"),
  CAN_DOC_THUMB("CanDocThumb"),
  UNKNOWN("Unknown");

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
    if (value == null || value.isBlank()) {
      return NONE;
    }
    for (PreviewStatus status : values()) {
      if (status.value.equalsIgnoreCase(value)) {
        return status;
      }
    }
    log.warn("Unsupported ShareFile PreviewStatus value received: {}", value);
    return UNKNOWN;
  }
}
