package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Request payload for cloning access-control assignments into another location. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class CloneRequest {

  @JsonProperty("FolderId")
  private String folderId;

  @JsonProperty("PrincipalId")
  private String principalId;

  @JsonProperty("Overwrite")
  private Boolean overwrite;

  @JsonProperty("ClonePrincipalIds")
  private List<String> clonePrincipalIds;
}
