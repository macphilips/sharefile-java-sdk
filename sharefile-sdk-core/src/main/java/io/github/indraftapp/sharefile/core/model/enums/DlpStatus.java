package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Represents ShareFile data-loss-prevention scan status values. */
public enum DlpStatus {
  UNSCANNED("Unscanned"),
  SCANNED_OK("ScannedOK"),
  SCANNED_REJECTED("ScannedRejected");

  private final String value;

  DlpStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static DlpStatus fromValue(String value) {
    for (DlpStatus status : values()) {
      if (status.value.equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown DlpStatus: " + value);
  }
}
