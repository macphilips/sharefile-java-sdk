package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Represents a favorite item entry for a ShareFile user. */
@Getter
@Setter
public class Favorite extends ODataEntity {

  @JsonProperty("Item")
  private Item item;

  @JsonProperty("CreationDate")
  private Instant creationDate;
}
