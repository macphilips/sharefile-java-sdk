package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Response payload describing the status of an uploaded chunk. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ChunkResult {

  @JsonProperty("ChunkNumber")
  private Integer chunkNumber;

  @JsonProperty("IsComplete")
  private Boolean isComplete;

  @JsonProperty("Hash")
  private String hash;
}
