package io.github.indraftapp.sharefile.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;

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

    @JsonIgnore
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
     * Computes the expiration timestamp based on the current time and the expiresIn value.
     */
    public void computeExpiresAt() {
        if (expiresIn != null) {
            this.expiresAt = Instant.now().plusSeconds(expiresIn);
        }
    }

    /**
     * Returns true if the token has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Returns true if the token will expire within the given buffer duration.
     */
    public boolean isExpiringSoon(Duration buffer) {
        if (expiresAt == null) {
            return false;
        }
        return Instant.now().plus(buffer).isAfter(expiresAt);
    }
}
