package io.github.indraftapp.sharefile.core.model;

/**
 * Represents a lightweight health snapshot for the SDK's ShareFile connectivity state.
 *
 * @param up whether the SDK currently considers the ShareFile connection healthy
 * @param subdomain the ShareFile subdomain associated with the status, when known
 * @param tokenExpiresInSeconds the remaining token lifetime in seconds, when known
 * @param error the latest health-check error message, when unhealthy
 */
public record HealthStatus(boolean up, String subdomain, Long tokenExpiresInSeconds, String error) {

  /**
   * Creates a healthy status snapshot for the given ShareFile subdomain.
   *
   * @param subdomain the ShareFile subdomain that was checked
   * @param tokenExpiry the remaining token lifetime in seconds
   * @return a healthy status snapshot
   */
  public static HealthStatus up(String subdomain, long tokenExpiry) {
    return new HealthStatus(true, subdomain, tokenExpiry, null);
  }

  /**
   * Creates an unhealthy status snapshot from the supplied failure cause.
   *
   * @param cause the exception that caused the health check to fail
   * @return an unhealthy status snapshot
   */
  public static HealthStatus down(Exception cause) {
    return new HealthStatus(false, null, null, cause.getMessage());
  }
}
