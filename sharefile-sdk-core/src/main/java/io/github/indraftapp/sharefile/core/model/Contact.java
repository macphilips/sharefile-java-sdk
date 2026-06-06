package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Represents a ShareFile contact. */
@Getter
@Setter
public class Contact extends ODataEntity {

  @JsonProperty("Email")
  private String email;

  @JsonProperty("FirstName")
  private String firstName;

  @JsonProperty("LastName")
  private String lastName;

  @JsonProperty("Company")
  private String company;
}
