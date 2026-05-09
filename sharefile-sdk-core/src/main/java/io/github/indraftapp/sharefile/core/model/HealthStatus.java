package io.github.indraftapp.sharefile.core.model;

public record HealthStatus(boolean up, String subdomain, Long tokenExpiresInSeconds, String error) {

    public static HealthStatus up(String subdomain, long tokenExpiry) {
        return new HealthStatus(true, subdomain, tokenExpiry, null);
    }

    public static HealthStatus down(Exception cause) {
        return new HealthStatus(false, null, null, cause.getMessage());
    }
}
