package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.jackson.ODataTypeResolver;
import io.github.indraftapp.sharefile.core.model.response.UserPreferences;
import io.github.indraftapp.sharefile.core.model.response.UserSecurity;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Zone getDefaultZone() {
        return defaultZone;
    }

    public void setDefaultZone(Zone defaultZone) {
        this.defaultZone = defaultZone;
    }

    public UserSecurity getSecurity() {
        return security;
    }

    public void setSecurity(UserSecurity security) {
        this.security = security;
    }

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferences preferences) {
        this.preferences = preferences;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
