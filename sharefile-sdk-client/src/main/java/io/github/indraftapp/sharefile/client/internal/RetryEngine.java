package io.github.indraftapp.sharefile.client.internal;

import io.github.indraftapp.sharefile.client.MetricNames;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.github.indraftapp.sharefile.core.exception.ShareFileApiException;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * Retry engine for ShareFile HTTP requests.
 *
 * <p>Wraps individual request executions and applies retry logic based on the response status code,
 * exception type, and HTTP method idempotency.
 *
 * <p>Package-private — not part of the public API.
 */
@Slf4j
final class RetryEngine {
  private static final Set<String> IDEMPOTENT_METHODS = Set.of("GET", "PUT", "HEAD");
  private static final int MAX_CONNECTION_RETRIES = 2;
  private static final List<Duration> RATE_LIMIT_DEFAULTS =
      List.of(Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(20));

  private final RetryConfig config;
  private final MetricsProvider metrics;

  RetryEngine(RetryConfig config, MetricsProvider metrics) {
    this.config = Objects.requireNonNull(config);
    this.metrics = Objects.requireNonNull(metrics);
  }

  /**
   * Executes a request with retry logic.
   *
   * @param action the action to execute
   * @param method the HTTP method (for idempotency checks)
   * @param policy per-request retry policy override
   * @return the successful HTTP response
   */
  HttpTransport.HttpResponse execute(RequestAction action, String method, RetryPolicy policy) {
    int maxRetries = resolveMaxRetries(policy);
    int attempt = 0;

    while (true) {
      try {
        return action.execute();
      } catch (ShareFileNetworkException e) {
        attempt++;
        if (!shouldRetryConnectionFailure(method, policy, attempt)) {
          throw e;
        }
        log.warn("Connection failure on attempt {}, retrying: {}", attempt, e.getMessage());
        metrics.incrementCounter(MetricNames.RETRY_ATTEMPTS, "reason", "connection_failure");
        sleepFor(Duration.ofSeconds(2));
      } catch (RetryableResponseException e) {
        attempt++;
        if (attempt > maxRetries) {
          throw e.toApiException();
        }
        if (!shouldRetryStatus(e.statusCode(), method, policy)) {
          throw e.toApiException();
        }
        Duration backoff = computeBackoff(e, attempt);
        log.warn(
            "HTTP {} on attempt {}, retrying after {}ms",
            e.statusCode(),
            attempt,
            backoff.toMillis());
        metrics.incrementCounter(MetricNames.RETRY_ATTEMPTS, "reason", "http_" + e.statusCode());
        sleepFor(backoff);
      }
    }
  }

  private int resolveMaxRetries(RetryPolicy policy) {
    if (policy != null && policy.getMaxRetries() >= 0) {
      return policy.getMaxRetries();
    }
    return config.getMaxRetries();
  }

  private boolean shouldRetryStatus(int status, String method, RetryPolicy policy) {
    if (!config.getRetryableStatuses().contains(status)) {
      return false;
    }
    // Retry only idempotent methods unless explicitly opted in for non-idempotent calls.
    return isIdempotent(method) || isNonIdempotentRetryAllowed(policy);
  }

  private boolean shouldRetryConnectionFailure(String method, RetryPolicy policy, int attempt) {
    if (!config.isRetryOnConnectionFailure()) {
      return false;
    }
    if (attempt > MAX_CONNECTION_RETRIES) {
      return false;
    }
    return isIdempotent(method) || isNonIdempotentRetryAllowed(policy);
  }

  private Duration computeBackoff(RetryableResponseException e, int attempt) {
    if (e.statusCode() == 429) {
      if (e.retryAfterSeconds() > 0) {
        return Duration.ofSeconds(e.retryAfterSeconds());
      }
      int idx = Math.min(attempt - 1, RATE_LIMIT_DEFAULTS.size() - 1);
      return RATE_LIMIT_DEFAULTS.get(idx);
    }
    // Exponential backoff with jitter for 5xx
    long baseMs = config.getInitialBackoff().toMillis();
    double multiplied = baseMs * Math.pow(config.getBackoffMultiplier(), attempt - 1);
    double jitter = multiplied * config.getJitterFactor();
    double jittered;
    if (jitter > 0) {
      jittered = multiplied + ThreadLocalRandom.current().nextDouble(-jitter, jitter);
    } else {
      jittered = multiplied;
    }
    return Duration.ofMillis(Math.max(0, (long) jittered));
  }

  private static boolean isIdempotent(String method) {
    return IDEMPOTENT_METHODS.contains(method.toUpperCase());
  }

  private static boolean isNonIdempotentRetryAllowed(RetryPolicy policy) {
    return policy != null && policy.isRetryNonIdempotent();
  }

  static void sleepFor(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ShareFileNetworkException("Interrupted during retry backoff", e);
    }
  }

  /** Functional interface for the action to execute (and potentially retry). */
  @FunctionalInterface
  interface RequestAction {
    HttpTransport.HttpResponse execute();
  }

  /**
   * Signals a retryable HTTP response. Carries status code and parsed error details so the retry
   * engine can make decisions without re-parsing.
   */
  static final class RetryableResponseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int statusCode;
    private final long retryAfterSeconds;
    private final String errorCode;
    private final String errorMessage;
    private final String requestId;
    private final String requestMethod;
    private final String requestUri;

    RetryableResponseException(
        int statusCode,
        long retryAfterSeconds,
        String errorCode,
        String errorMessage,
        String requestId,
        String requestMethod,
        String requestUri) {
      super("HTTP %d: %s".formatted(statusCode, errorMessage));
      this.statusCode = statusCode;
      this.retryAfterSeconds = retryAfterSeconds;
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
      this.requestId = requestId;
      this.requestMethod = requestMethod;
      this.requestUri = requestUri;
    }

    int statusCode() {
      return statusCode;
    }

    long retryAfterSeconds() {
      return retryAfterSeconds;
    }

    ShareFileApiException toApiException() {
      return ErrorResponseMapper.create(
          statusCode,
          errorCode,
          errorMessage,
          requestId,
          requestMethod,
          requestUri,
          retryAfterSeconds);
    }
  }
}
