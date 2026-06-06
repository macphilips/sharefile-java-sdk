package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Response payload describing the final result of a ShareFile upload. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class UploadResult {

  @JsonProperty("ItemId")
  private String itemId;

  @JsonProperty("FileName")
  private String fileName;

  @JsonProperty("FileSize")
  private Long fileSize;

  @JsonProperty("Details")
  private String details;
}
