package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class Favorite extends ODataEntity {

    @JsonProperty("Item")
    private Item item;

    @JsonProperty("CreationDate")
    private Instant creationDate;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }
}
