package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a metadata entry associated with a ShareFile entity.
 */
@Getter
@Setter
public class Metadata extends ODataEntity {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Value")
    private String value;

    @JsonProperty("IsPublic")
    private Boolean isPublic;
}
