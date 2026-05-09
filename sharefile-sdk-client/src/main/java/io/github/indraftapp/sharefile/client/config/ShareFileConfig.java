package io.github.indraftapp.sharefile.client.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for the ShareFile SDK client.
 *
 * <p>Holds connection settings, timeouts, and the computed base API URL.
 * Create instances via the {@link Builder}:
 * <pre>{@code
 * ShareFileConfig config = ShareFileConfig.builder()
 *     .subdomain("mycompany")
 *     .connectTimeout(Duration.ofSeconds(15))
 *     .build();
 * }</pre>
 */
public final class ShareFileConfig {

    private static final String DEFAULT_API_CONTROL_PLANE = "sharefile.com";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_UPLOAD_TIMEOUT = Duration.ofSeconds(300);
    private static final Duration DEFAULT_DOWNLOAD_TIMEOUT = Duration.ofSeconds(300);
    private static final Duration DEFAULT_TOKEN_REFRESH_BUFFER = Duration.ofMinutes(5);

    private final String subdomain;
    private final String apiControlPlane;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final Duration uploadTimeout;
    private final Duration downloadTimeout;
    private final Duration tokenRefreshBuffer;
    private final String baseUrl;

    private ShareFileConfig(Builder builder) {
        this.subdomain = Objects.requireNonNull(builder.subdomain, "subdomain must not be null");
        this.apiControlPlane = builder.apiControlPlane;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.uploadTimeout = builder.uploadTimeout;
        this.downloadTimeout = builder.downloadTimeout;
        this.tokenRefreshBuffer = builder.tokenRefreshBuffer;
        this.baseUrl = "https://%s.sf-api.com/sf/v3".formatted(subdomain);
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getSubdomain() {
        return subdomain;
    }

    public String getApiControlPlane() {
        return apiControlPlane;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public Duration getUploadTimeout() {
        return uploadTimeout;
    }

    public Duration getDownloadTimeout() {
        return downloadTimeout;
    }

    public Duration getTokenRefreshBuffer() {
        return tokenRefreshBuffer;
    }

    /**
     * Returns the computed base API URL, e.g. {@code https://mycompany.sf-api.com/sf/v3}.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the token endpoint URL, e.g.
     * {@code https://mycompany.sharefile.com/oauth/token}.
     */
    public String getTokenEndpointUrl() {
        return "https://%s.%s/oauth/token".formatted(subdomain, apiControlPlane);
    }

    /**
     * Mutable builder for {@link ShareFileConfig}.
     */
    public static final class Builder {

        private String subdomain;
        private String apiControlPlane = DEFAULT_API_CONTROL_PLANE;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private Duration uploadTimeout = DEFAULT_UPLOAD_TIMEOUT;
        private Duration downloadTimeout = DEFAULT_DOWNLOAD_TIMEOUT;
        private Duration tokenRefreshBuffer = DEFAULT_TOKEN_REFRESH_BUFFER;

        private Builder() {}

        /**
         * Sets the ShareFile account subdomain (required).
         *
         * @param subdomain the tenant subdomain, e.g. {@code "mycompany"}
         * @return this builder
         */
        public Builder subdomain(String subdomain) {
            this.subdomain = Objects.requireNonNull(subdomain, "subdomain must not be null");
            return this;
        }

        /**
         * Sets the API control plane hostname. Defaults to {@code "sharefile.com"}.
         *
         * @param apiControlPlane the control plane hostname
         * @return this builder
         */
        public Builder apiControlPlane(String apiControlPlane) {
            this.apiControlPlane = Objects.requireNonNull(apiControlPlane, "apiControlPlane must not be null");
            return this;
        }

        /**
         * Sets the connect timeout. Defaults to 10 seconds.
         *
         * @param connectTimeout the connect timeout duration
         * @return this builder
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
            return this;
        }

        /**
         * Sets the read timeout for JSON API calls. Defaults to 30 seconds.
         *
         * @param readTimeout the read timeout duration
         * @return this builder
         */
        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout must not be null");
            return this;
        }

        /**
         * Sets the upload timeout. Defaults to 300 seconds.
         *
         * @param uploadTimeout the upload timeout duration
         * @return this builder
         */
        public Builder uploadTimeout(Duration uploadTimeout) {
            this.uploadTimeout = Objects.requireNonNull(uploadTimeout, "uploadTimeout must not be null");
            return this;
        }

        /**
         * Sets the download timeout. Defaults to 300 seconds.
         *
         * @param downloadTimeout the download timeout duration
         * @return this builder
         */
        public Builder downloadTimeout(Duration downloadTimeout) {
            this.downloadTimeout = Objects.requireNonNull(downloadTimeout, "downloadTimeout must not be null");
            return this;
        }

        /**
         * Sets the buffer duration for proactive token refresh. Defaults to 5 minutes.
         *
         * <p>The token will be refreshed proactively when 80% of the
         * {@code expires_in} duration has elapsed.
         *
         * @param tokenRefreshBuffer the token refresh buffer duration
         * @return this builder
         */
        public Builder tokenRefreshBuffer(Duration tokenRefreshBuffer) {
            this.tokenRefreshBuffer = Objects.requireNonNull(tokenRefreshBuffer, "tokenRefreshBuffer must not be null");
            return this;
        }

        /**
         * Builds an immutable {@link ShareFileConfig}.
         *
         * @throws NullPointerException if subdomain has not been set
         */
        public ShareFileConfig build() {
            return new ShareFileConfig(this);
        }
    }
}
