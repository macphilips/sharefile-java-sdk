package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.response.AccountPreferences;
import io.github.indraftapp.sharefile.core.model.response.Branding;
import io.github.indraftapp.sharefile.core.model.response.SSOAccountProvider;

public class Account extends ODataEntity {

    @JsonProperty("Subdomain")
    private String subdomain;

    @JsonProperty("Preferences")
    private AccountPreferences preferences;

    @JsonProperty("Branding")
    private Branding branding;

    @JsonProperty("SSO")
    private SSOAccountProvider sso;

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public AccountPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(AccountPreferences preferences) {
        this.preferences = preferences;
    }

    public Branding getBranding() {
        return branding;
    }

    public void setBranding(Branding branding) {
        this.branding = branding;
    }

    public SSOAccountProvider getSso() {
        return sso;
    }

    public void setSso(SSOAccountProvider sso) {
        this.sso = sso;
    }
}
