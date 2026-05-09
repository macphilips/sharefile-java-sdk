package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Response payload describing SSO provider settings for a ShareFile account.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SSOAccountProvider {

    @JsonProperty("LogoutUrl")
    private String logoutUrl;

    @JsonProperty("LoginUrl")
    private String loginUrl;

    @JsonProperty("IPRestrictions")
    private String ipRestrictions;

    @JsonProperty("ForceSSO")
    private Boolean forceSSO;

    @JsonProperty("EntityID")
    private String entityID;

    @JsonProperty("SFEntityID")
    private String sfEntityID;
}
