package io.github.indraftapp.sharefile.client.spi;

import io.github.indraftapp.sharefile.core.model.enums.GrantType;
import java.util.Objects;

/**
 * Holds the credentials needed to authenticate with the ShareFile OAuth2 token endpoint.
 *
 * <p>Create instances via the fluent {@link Builder}:
 *
 * <pre>{@code
 * Credentials creds = Credentials.builder()
 *     .clientCredentials("my-client-id", "my-client-secret")
 *     .passwordGrant("user@example.com", "password")
 *     .build();
 * }</pre>
 *
 * <p>{@code clientId} and {@code clientSecret} are always required. Grant-type-specific fields
 * ({@code username}/{@code password} for PASSWORD, {@code authorizationCode} for
 * AUTHORIZATION_CODE) are validated at build time.
 */
public record Credentials(
    String clientId,
    String clientSecret,
    GrantType grantType,
    String username,
    String password,
    String authorizationCode,
    String authorizationCodeRedirectUri) {

  /** Validates required fields after construction. */
  public Credentials {
    Objects.requireNonNull(clientId, "clientId must not be null");
    Objects.requireNonNull(clientSecret, "clientSecret must not be null");
    Objects.requireNonNull(grantType, "grantType must not be null");

    switch (grantType) {
      case PASSWORD -> {
        Objects.requireNonNull(username, "username must not be null for PASSWORD grant");
        Objects.requireNonNull(password, "password must not be null for PASSWORD grant");
      }
      case AUTHORIZATION_CODE -> {
        if (authorizationCode == null && authorizationCodeRedirectUri == null) {
          throw new NullPointerException(
              "authorizationCode or authorizationCodeRedirectUri must not be null for AUTHORIZATION_CODE grant");
        }
      }
      default ->
          throw new IllegalArgumentException(
              "Unsupported grant type for credentials: " + grantType);
    }
  }

  /** Creates a new builder for {@link Credentials}. */
  public static Builder builder() {
    return new Builder();
  }

  /** Fluent builder for {@link Credentials}. */
  public static final class Builder {

    private String clientId;
    private String clientSecret;
    private GrantType grantType;
    private String username;
    private String password;
    private String authorizationCode;
    private String authorizationCodeRedirectUri;

    private Builder() {}

    /**
     * Sets the OAuth2 client credentials (always required).
     *
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @return this builder
     */
    public Builder clientCredentials(String clientId, String clientSecret) {
      this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
      this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
      return this;
    }

    /**
     * Configures a password grant with the given username and password.
     *
     * <p>Password grant is intended for legacy service-account workflows, internal automation, and
     * development/testing scenarios. Authorization-code OAuth is preferred for production.
     *
     * @param username the ShareFile username
     * @param password the ShareFile password
     * @return this builder
     */
    public Builder passwordGrant(String username, String password) {
      this.grantType = GrantType.PASSWORD;
      this.username = Objects.requireNonNull(username, "username must not be null");
      this.password = Objects.requireNonNull(password, "password must not be null");
      return this;
    }

    /**
     * Configures an authorization-code grant with the given code.
     *
     * @param code the authorization code from the OAuth2 redirect
     * @return this builder
     */
    public Builder authorizationCode(String code) {
      this.grantType = GrantType.AUTHORIZATION_CODE;
      this.authorizationCode = Objects.requireNonNull(code, "code must not be null");
      this.authorizationCodeRedirectUri = null;
      return this;
    }

    /**
     * Configures an authorization-code grant using the full redirect URI returned by ShareFile.
     *
     * <p>This allows the SDK to validate the `h` signature before exchanging the code.
     *
     * @param redirectUri the full redirect URI including the `code` and `h` parameters
     * @return this builder
     */
    public Builder authorizationCodeRedirectUri(String redirectUri) {
      this.grantType = GrantType.AUTHORIZATION_CODE;
      this.authorizationCodeRedirectUri =
          Objects.requireNonNull(redirectUri, "redirectUri must not be null");
      this.authorizationCode = null;
      return this;
    }

    /**
     * Builds the {@link Credentials} instance, validating that all required fields are set.
     *
     * @return the validated credentials
     * @throws NullPointerException if required fields are missing
     * @throws IllegalArgumentException if the grant type is unsupported
     */
    public Credentials build() {
      return new Credentials(
          clientId,
          clientSecret,
          grantType,
          username,
          password,
          authorizationCode,
          authorizationCodeRedirectUri);
    }
  }
}
