package io.github.indraftapp.sharefile.core.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Represents ShareFile tree-operation modes for copy, move, and manage workflows. */
public enum TreeMode {
  COPY("Copy"),
  MOVE("Move"),
  MANAGE("Manage");

  private final String value;

  TreeMode(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static TreeMode fromValue(String value) {
    for (TreeMode mode : values()) {
      if (mode.value.equalsIgnoreCase(value)) {
        return mode;
      }
    }
    throw new IllegalArgumentException("Unknown TreeMode: " + value);
  }
}
