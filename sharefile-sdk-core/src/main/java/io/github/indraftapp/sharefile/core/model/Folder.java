package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.response.ItemInfo;
import io.github.indraftapp.sharefile.core.model.response.Redirection;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a ShareFile folder item and its folder-specific metadata.
 */
@Getter
@Setter
public class Folder extends Item {

    @JsonProperty("FileCount")
    private Integer fileCount;

    @JsonProperty("Children")
    private List<Item> children;

    @JsonProperty("HasRemoteChildren")
    private Boolean hasRemoteChildren;

    @JsonProperty("Info")
    private ItemInfo info;

    @JsonProperty("Redirection")
    private Redirection redirection;
}
