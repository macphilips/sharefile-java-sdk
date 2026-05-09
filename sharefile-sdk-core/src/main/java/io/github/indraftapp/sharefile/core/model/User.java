package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.jackson.ODataTypeResolver;
import io.github.indraftapp.sharefile.core.model.response.UserPreferences;
import io.github.indraftapp.sharefile.core.model.response.UserSecurity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a ShareFile user entity.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CUSTOM,
        include = As.EXISTING_PROPERTY,
        property = "odata.type",
        visible = true,
        defaultImpl = User.class
)
@JsonTypeIdResolver(ODataTypeResolver.class)
@Getter
@Setter
public class User extends ODataEntity {

    @JsonProperty("Email")
    private String email;

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("LastName")
    private String lastName;

    @JsonProperty("Company")
    private String company;

    @JsonProperty("DefaultZone")
    private Zone defaultZone;

    @JsonProperty("Security")
    private UserSecurity security;

    @JsonProperty("Preferences")
    private UserPreferences preferences;

    @JsonProperty("Roles")
    private List<String> roles;
}
