package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.response.ItemInfo;
import io.github.indraftapp.sharefile.core.model.response.Redirection;

import java.util.List;

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

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public List<Item> getChildren() {
        return children;
    }

    public void setChildren(List<Item> children) {
        this.children = children;
    }

    public Boolean getHasRemoteChildren() {
        return hasRemoteChildren;
    }

    public void setHasRemoteChildren(Boolean hasRemoteChildren) {
        this.hasRemoteChildren = hasRemoteChildren;
    }

    public ItemInfo getInfo() {
        return info;
    }

    public void setInfo(ItemInfo info) {
        this.info = info;
    }

    public Redirection getRedirection() {
        return redirection;
    }

    public void setRedirection(Redirection redirection) {
        this.redirection = redirection;
    }
}
