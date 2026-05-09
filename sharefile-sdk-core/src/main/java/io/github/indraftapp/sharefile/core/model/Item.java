package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.jackson.ODataTypeResolver;
import io.github.indraftapp.sharefile.core.model.enums.PreviewStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Represents a ShareFile item entity, including files, folders, notes, and links.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CUSTOM,
        include = As.EXISTING_PROPERTY,
        property = "odata.type",
        visible = true,
        defaultImpl = Item.class
)
@JsonTypeIdResolver(ODataTypeResolver.class)
@Getter
@Setter
public class Item extends ODataEntity {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("FileName")
    private String fileName;

    @JsonProperty("Creator")
    private User creator;

    @JsonProperty("Parent")
    private Item parent;

    @JsonProperty("AccessControls")
    private List<AccessControl> accessControls;

    @JsonProperty("Zone")
    private Zone zone;

    @JsonProperty("CreationDate")
    private Instant creationDate;

    @JsonProperty("ProgenyEditDate")
    private Instant progenyEditDate;

    @JsonProperty("LastModifiedByUserID")
    private String lastModifiedByUserID;

    @JsonProperty("ClientCreatedDate")
    private Instant clientCreatedDate;

    @JsonProperty("ClientModifiedDate")
    private Instant clientModifiedDate;

    @JsonProperty("ExpirationDate")
    private Instant expirationDate;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("DiskSpaceLimit")
    private Integer diskSpaceLimit;

    @JsonProperty("IsHidden")
    private Boolean isHidden;

    @JsonProperty("BandwidthLimitInMB")
    private Integer bandwidthLimitInMB;

    @JsonProperty("Owner")
    private User owner;

    @JsonProperty("Account")
    private Account account;

    @JsonProperty("FileSizeInKB")
    private Integer fileSizeInKB;

    @JsonProperty("FileSizeBytes")
    private Long fileSizeBytes;

    @JsonProperty("Path")
    private String path;

    @JsonProperty("CreatorFirstName")
    private String creatorFirstName;

    @JsonProperty("CreatorLastName")
    private String creatorLastName;

    @JsonProperty("ExpirationDays")
    private Integer expirationDays;

    @JsonProperty("PreviewStatus")
    private PreviewStatus previewStatus;

    @JsonProperty("HasPendingDeletion")
    private Boolean hasPendingDeletion;

    @JsonProperty("AssociatedFolderTemplateID")
    private String associatedFolderTemplateID;

    @JsonProperty("IsTemplateOwned")
    private Boolean isTemplateOwned;

    @JsonProperty("StreamID")
    private String streamID;

    @JsonProperty("HasMultipleVersions")
    private Boolean hasMultipleVersions;

    @JsonProperty("HasPendingAsyncOp")
    private Boolean hasPendingAsyncOp;

    @JsonProperty("Metadata")
    private List<Metadata> itemMetadata;

    @JsonProperty("Favorite")
    private Favorite favorite;

    @JsonProperty("SemanticPath")
    private String semanticPath;
}
