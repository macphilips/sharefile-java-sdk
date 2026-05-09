package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Represents a ShareFile group and its associated contacts. */
@Getter
@Setter
public class Group extends ODataEntity {

  @JsonProperty("Name")
  private String name;

  @JsonProperty("IsShared")
  private Boolean isShared;

  @JsonProperty("Contacts")
  private List<Contact> contacts;
}
