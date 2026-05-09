package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Response payload containing ShareFile download URLs and preparation state. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class DownloadSpecification {

  @JsonProperty("DownloadUrl")
  private String downloadUrl;

  @JsonProperty("DownloadToken")
  private String downloadToken;

  @JsonProperty("PrepStatus")
  private String prepStatus;
}
