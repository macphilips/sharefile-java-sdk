package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.jackson.ODataTypeResolver;
import io.github.indraftapp.sharefile.core.model.enums.PreviewStatus;

import java.time.Instant;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CUSTOM,
        include = As.EXISTING_PROPERTY,
        property = "odata.type",
        visible = true,
        defaultImpl = Item.class
)
@JsonTypeIdResolver(ODataTypeResolver.class)
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Item getParent() {
        return parent;
    }

    public void setParent(Item parent) {
        this.parent = parent;
    }

    public List<AccessControl> getAccessControls() {
        return accessControls;
    }

    public void setAccessControls(List<AccessControl> accessControls) {
        this.accessControls = accessControls;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public Instant getProgenyEditDate() {
        return progenyEditDate;
    }

    public void setProgenyEditDate(Instant progenyEditDate) {
        this.progenyEditDate = progenyEditDate;
    }

    public String getLastModifiedByUserID() {
        return lastModifiedByUserID;
    }

    public void setLastModifiedByUserID(String lastModifiedByUserID) {
        this.lastModifiedByUserID = lastModifiedByUserID;
    }

    public Instant getClientCreatedDate() {
        return clientCreatedDate;
    }

    public void setClientCreatedDate(Instant clientCreatedDate) {
        this.clientCreatedDate = clientCreatedDate;
    }

    public Instant getClientModifiedDate() {
        return clientModifiedDate;
    }

    public void setClientModifiedDate(Instant clientModifiedDate) {
        this.clientModifiedDate = clientModifiedDate;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDiskSpaceLimit() {
        return diskSpaceLimit;
    }

    public void setDiskSpaceLimit(Integer diskSpaceLimit) {
        this.diskSpaceLimit = diskSpaceLimit;
    }

    public Boolean getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }

    public Integer getBandwidthLimitInMB() {
        return bandwidthLimitInMB;
    }

    public void setBandwidthLimitInMB(Integer bandwidthLimitInMB) {
        this.bandwidthLimitInMB = bandwidthLimitInMB;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Integer getFileSizeInKB() {
        return fileSizeInKB;
    }

    public void setFileSizeInKB(Integer fileSizeInKB) {
        this.fileSizeInKB = fileSizeInKB;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCreatorFirstName() {
        return creatorFirstName;
    }

    public void setCreatorFirstName(String creatorFirstName) {
        this.creatorFirstName = creatorFirstName;
    }

    public String getCreatorLastName() {
        return creatorLastName;
    }

    public void setCreatorLastName(String creatorLastName) {
        this.creatorLastName = creatorLastName;
    }

    public Integer getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(Integer expirationDays) {
        this.expirationDays = expirationDays;
    }

    public PreviewStatus getPreviewStatus() {
        return previewStatus;
    }

    public void setPreviewStatus(PreviewStatus previewStatus) {
        this.previewStatus = previewStatus;
    }

    public Boolean getHasPendingDeletion() {
        return hasPendingDeletion;
    }

    public void setHasPendingDeletion(Boolean hasPendingDeletion) {
        this.hasPendingDeletion = hasPendingDeletion;
    }

    public String getAssociatedFolderTemplateID() {
        return associatedFolderTemplateID;
    }

    public void setAssociatedFolderTemplateID(String associatedFolderTemplateID) {
        this.associatedFolderTemplateID = associatedFolderTemplateID;
    }

    public Boolean getIsTemplateOwned() {
        return isTemplateOwned;
    }

    public void setIsTemplateOwned(Boolean isTemplateOwned) {
        this.isTemplateOwned = isTemplateOwned;
    }

    public String getStreamID() {
        return streamID;
    }

    public void setStreamID(String streamID) {
        this.streamID = streamID;
    }

    public Boolean getHasMultipleVersions() {
        return hasMultipleVersions;
    }

    public void setHasMultipleVersions(Boolean hasMultipleVersions) {
        this.hasMultipleVersions = hasMultipleVersions;
    }

    public Boolean getHasPendingAsyncOp() {
        return hasPendingAsyncOp;
    }

    public void setHasPendingAsyncOp(Boolean hasPendingAsyncOp) {
        this.hasPendingAsyncOp = hasPendingAsyncOp;
    }

    public List<Metadata> getItemMetadata() {
        return itemMetadata;
    }

    public void setItemMetadata(List<Metadata> itemMetadata) {
        this.itemMetadata = itemMetadata;
    }

    public Favorite getFavorite() {
        return favorite;
    }

    public void setFavorite(Favorite favorite) {
        this.favorite = favorite;
    }

    public String getSemanticPath() {
        return semanticPath;
    }

    public void setSemanticPath(String semanticPath) {
        this.semanticPath = semanticPath;
    }
}
