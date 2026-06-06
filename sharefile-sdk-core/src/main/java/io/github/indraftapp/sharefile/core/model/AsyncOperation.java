package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Represents a server-side ShareFile asynchronous operation. */
@Getter
@Setter
public class AsyncOperation extends ODataEntity {

  @JsonProperty("State")
  private String state;

  @JsonProperty("BatchId")
  private String batchId;

  @JsonProperty("Progress")
  private Integer progress;

  /**
   * Returns {@code true} when the operation has reached a terminal state and no further polling is
   * required.
   *
   * @return {@code true} when the operation state is terminal
   */
  public boolean isTerminal() {
    return "Completed".equals(state) || "Error".equals(state) || "Cancelled".equals(state);
  }
}
