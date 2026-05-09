package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Represents a device known to a ShareFile account. */
@Getter
@Setter
public class Device extends ODataEntity {

  @JsonProperty("DeviceName")
  private String deviceName;

  @JsonProperty("DeviceType")
  private String deviceType;

  @JsonProperty("User")
  private User user;
}
