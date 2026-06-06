package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;

/** Represents an OAuth token response returned by ShareFile authentication endpoints. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthToken {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("apicp")
  private String apiControlPlane;

  @JsonProperty("appcp")
  private String appControlPlane;

  @JsonProperty("subdomain")
  private String subdomain;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("expires_at")
  private Instant expiresAt;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public String getApiControlPlane() {
    return apiControlPlane;
  }

  public void setApiControlPlane(String apiControlPlane) {
    this.apiControlPlane = apiControlPlane;
  }

  public String getAppControlPlane() {
    return appControlPlane;
  }

  public void setAppControlPlane(String appControlPlane) {
    this.appControlPlane = appControlPlane;
  }

  public String getSubdomain() {
    return subdomain;
  }

  public void setSubdomain(String subdomain) {
    this.subdomain = subdomain;
  }

  public Long getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Long expiresIn) {
    this.expiresIn = expiresIn;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  /**
   * Computes the absolute expiration timestamp from the current clock time and the {@code
   * expires_in} value.
   */
  public void computeExpiresAt() {
    if (expiresIn != null) {
      this.expiresAt = Instant.now().plusSeconds(expiresIn);
    }
  }

  /** Returns {@code true} when the token expiration timestamp is known and already in the past. */
  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  /** Returns {@code true} when the token will expire within the supplied buffer duration. */
  public boolean isExpiringSoon(Duration buffer) {
    if (expiresAt == null) {
      return false;
    }
    return Instant.now().plus(buffer).isAfter(expiresAt);
  }
}
