package io.github.indraftapp.sharefile.client.retry;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for the retry engine.
 *
 * <p>Create instances via {@link #defaults()} or the fluent {@link Builder}:
 *
 * <pre>{@code
 * RetryConfig config = RetryConfig.builder()
 *     .maxRetries(5)
 *     .backoffMultiplier(2.0)
 *     .build();
 * }</pre>
 */
public final class RetryConfig {

  private static final int DEFAULT_MAX_RETRIES = 3;
  private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofSeconds(1);
  private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
  private static final double DEFAULT_JITTER_FACTOR = 0.2;
  private static final Set<Integer> DEFAULT_RETRYABLE_STATUSES = Set.of(429, 500, 502, 503, 504);
  private static final boolean DEFAULT_RETRY_ON_CONNECTION_FAILURE = true;

  private final int maxRetries;
  private final Duration initialBackoff;
  private final double backoffMultiplier;
  private final double jitterFactor;
  private final Set<Integer> retryableStatuses;
  private final boolean retryOnConnectionFailure;

  private RetryConfig(Builder builder) {
    this.maxRetries = builder.maxRetries;
    this.initialBackoff = builder.initialBackoff;
    this.backoffMultiplier = builder.backoffMultiplier;
    this.jitterFactor = builder.jitterFactor;
    this.retryableStatuses = Collections.unmodifiableSet(builder.retryableStatuses);
    this.retryOnConnectionFailure = builder.retryOnConnectionFailure;
  }

  /** Returns a RetryConfig with default values. */
  public static RetryConfig defaults() {
    return builder().build();
  }

  /** Creates a new builder. */
  public static Builder builder() {
    return new Builder();
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public Duration getInitialBackoff() {
    return initialBackoff;
  }

  public double getBackoffMultiplier() {
    return backoffMultiplier;
  }

  public double getJitterFactor() {
    return jitterFactor;
  }

  public Set<Integer> getRetryableStatuses() {
    return retryableStatuses;
  }

  public boolean isRetryOnConnectionFailure() {
    return retryOnConnectionFailure;
  }

  /** Mutable builder for {@link RetryConfig}. */
  public static final class Builder {
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private Duration initialBackoff = DEFAULT_INITIAL_BACKOFF;
    private double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
    private double jitterFactor = DEFAULT_JITTER_FACTOR;
    private Set<Integer> retryableStatuses = DEFAULT_RETRYABLE_STATUSES;
    private boolean retryOnConnectionFailure = DEFAULT_RETRY_ON_CONNECTION_FAILURE;

    private Builder() {}

    public Builder maxRetries(int maxRetries) {
      if (maxRetries < 0) {
        throw new IllegalArgumentException("maxRetries must be >= 0");
      }
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder initialBackoff(Duration initialBackoff) {
      this.initialBackoff = Objects.requireNonNull(initialBackoff);
      return this;
    }

    public Builder backoffMultiplier(double backoffMultiplier) {
      if (backoffMultiplier < 1.0) {
        throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
      }
      this.backoffMultiplier = backoffMultiplier;
      return this;
    }

    public Builder jitterFactor(double jitterFactor) {
      if (jitterFactor < 0.0 || jitterFactor > 1.0) {
        throw new IllegalArgumentException("jitterFactor must be between 0.0 and 1.0");
      }
      this.jitterFactor = jitterFactor;
      return this;
    }

    public Builder retryableStatuses(Set<Integer> retryableStatuses) {
      this.retryableStatuses = Objects.requireNonNull(retryableStatuses);
      return this;
    }

    public Builder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
      this.retryOnConnectionFailure = retryOnConnectionFailure;
      return this;
    }

    public RetryConfig build() {
      return new RetryConfig(this);
    }
  }
}
