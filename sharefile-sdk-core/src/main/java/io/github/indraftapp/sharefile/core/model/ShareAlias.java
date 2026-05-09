package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a ShareFile share recipient or alias entry.
 */
@Getter
@Setter
public class ShareAlias extends ODataEntity {

    @JsonProperty("Email")
    private String email;

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("LastName")
    private String lastName;

    @JsonProperty("Company")
    private String company;

    @JsonProperty("DownloadCount")
    private Integer downloadCount;
}
