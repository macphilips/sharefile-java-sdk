package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.indraftapp.sharefile.core.model.response.AccountPreferences;
import io.github.indraftapp.sharefile.core.model.response.Branding;
import io.github.indraftapp.sharefile.core.model.response.SSOAccountProvider;
import lombok.Getter;
import lombok.Setter;

/** Represents a ShareFile account and its account-level settings. */
@Getter
@Setter
public class Account extends ODataEntity {

  @JsonProperty("Subdomain")
  private String subdomain;

  @JsonProperty("Preferences")
  private AccountPreferences preferences;

  @JsonProperty("Branding")
  private Branding branding;

  @JsonProperty("SSO")
  private SSOAccountProvider sso;
}
