package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.enums.UploadMethod;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Request payload for negotiating ShareFile upload behavior and metadata. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class UploadRequestParams {

  @JsonProperty("Method")
  private UploadMethod method;

  @JsonProperty("FileName")
  private String fileName;

  @JsonProperty("FileSize")
  private Long fileSize;

  @JsonProperty("Details")
  private String details;

  @JsonProperty("IsSend")
  private Boolean isSend;

  @JsonProperty("ThreadCount")
  private Integer threadCount;

  @JsonProperty("Overwrite")
  private Boolean overwrite;

  @JsonProperty("Title")
  private String title;

  @JsonProperty("Tool")
  private String tool;

  @JsonProperty("Raw")
  private Boolean raw;

  @JsonProperty("CanResume")
  private Boolean canResume;

  @JsonProperty("BatchId")
  private String batchId;

  @JsonProperty("BatchLast")
  private Boolean batchLast;

  @JsonProperty("Notify")
  private Boolean notify;

  @JsonProperty("ClientCreatedDate")
  private Instant clientCreatedDate;

  @JsonProperty("ClientModifiedDate")
  private Instant clientModifiedDate;

  @JsonProperty("ExpirationDays")
  private Integer expirationDays;
}
