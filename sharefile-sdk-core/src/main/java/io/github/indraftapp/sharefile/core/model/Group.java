package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a ShareFile group and its associated contacts.
 */
@Getter
@Setter
public class Group extends ODataEntity {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("IsShared")
    private Boolean isShared;

    @JsonProperty("Contacts")
    private List<Contact> contacts;
}
