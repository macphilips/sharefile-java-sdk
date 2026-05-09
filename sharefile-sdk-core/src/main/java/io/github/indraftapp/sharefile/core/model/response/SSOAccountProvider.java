package io.github.indraftapp.sharefile.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getIpRestrictions() {
        return ipRestrictions;
    }

    public void setIpRestrictions(String ipRestrictions) {
        this.ipRestrictions = ipRestrictions;
    }

    public Boolean getForceSSO() {
        return forceSSO;
    }

    public void setForceSSO(Boolean forceSSO) {
        this.forceSSO = forceSSO;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public String getSfEntityID() {
        return sfEntityID;
    }

    public void setSfEntityID(String sfEntityID) {
        this.sfEntityID = sfEntityID;
    }
}
