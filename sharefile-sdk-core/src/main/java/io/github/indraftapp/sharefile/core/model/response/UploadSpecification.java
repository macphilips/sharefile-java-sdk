package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.enums.UploadMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Response payload containing ShareFile upload endpoints and resume metadata. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class UploadSpecification {

  @JsonProperty("Method")
  private UploadMethod method;

  @JsonProperty("ChunkUri")
  private String chunkUri;

  @JsonProperty("IsResume")
  private Boolean isResume;

  @JsonProperty("ResumeIndex")
  private Long resumeIndex;

  @JsonProperty("ResumeOffset")
  private Long resumeOffset;

  @JsonProperty("ResumeFileHash")
  private String resumeFileHash;

  @JsonProperty("FinishUri")
  private String finishUri;
}
