package io.github.indraftapp.sharefile.client.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.core.exception.ShareFileApiException;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import io.github.indraftapp.sharefile.core.exception.ShareFileServerException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link RetryEngine}. */
class RetryEngineTest {

  private RecordingMetricsProvider metricsProvider;

  @BeforeEach
  void setUp() {
    metricsProvider = new RecordingMetricsProvider();
  }

  // ── 5xx retry tests ───────────────────────────────────────────────────

  @Test
  void getOn500RetriesWithExponentialBackoff() {
    RetryConfig config =
        RetryConfig.builder()
            .maxRetries(3)
            .initialBackoff(Duration.ofMillis(10))
            .jitterFactor(0.0) // no jitter for deterministic test
            .build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    HttpTransport.HttpResponse response =
        engine.execute(
            () -> {
              int n = attempts.incrementAndGet();
              if (n <= 2) {
                throw retryable(500, "InternalError", "Server error");
              }
              return successResponse();
            },
            "GET",
            RetryPolicy.DEFAULT);

    assertEquals(200, response.statusCode());
    assertEquals(3, attempts.get());
    assertEquals(2, metricsProvider.counters.size()); // 2 retry counter increments
  }

  @Test
  void postOn500ThrowsImmediatelyWithDefaultPolicy() {
    RetryConfig config =
        RetryConfig.builder().maxRetries(3).initialBackoff(Duration.ofMillis(10)).build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    assertThrows(
        ShareFileServerException.class,
        () ->
            engine.execute(
                () -> {
                  attempts.incrementAndGet();
                  throw retryable(500, "InternalError", "Server error");
                },
                "POST",
                RetryPolicy.DEFAULT));

    assertEquals(1, attempts.get()); // Only 1 attempt, no retries
  }

  @Test
  void postOn500RetriesWhenOptedIn() {
    RetryConfig config =
        RetryConfig.builder()
            .maxRetries(3)
            .initialBackoff(Duration.ofMillis(10))
            .jitterFactor(0.0)
            .build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    HttpTransport.HttpResponse response =
        engine.execute(
            () -> {
              int n = attempts.incrementAndGet();
              if (n <= 1) {
                throw retryable(500, "InternalError", "Server error");
              }
              return successResponse();
            },
            "POST",
            RetryPolicy.retryOnServerError(2));

    assertEquals(200, response.statusCode());
    assertEquals(2, attempts.get());
  }

  @Test
  void maxRetriesExhaustedThrowsLastException() {
    RetryConfig config =
        RetryConfig.builder()
            .maxRetries(2)
            .initialBackoff(Duration.ofMillis(10))
            .jitterFactor(0.0)
            .build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    ShareFileServerException ex =
        assertThrows(
            ShareFileServerException.class,
            () ->
                engine.execute(
                    () -> {
                      attempts.incrementAndGet();
                      throw retryable(502, "BadGateway", "Unavailable");
                    },
                    "GET",
                    RetryPolicy.DEFAULT));

    assertEquals(3, attempts.get()); // 1 initial + 2 retries
    assertEquals(502, ex.getHttpStatus());
  }

  // ── 429 retry tests ───────────────────────────────────────────────────

  @Test
  void status429WithRetryAfterUsesHeaderValue() {
    RetryConfig config =
        RetryConfig.builder().maxRetries(3).initialBackoff(Duration.ofMillis(10)).build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();

    // 429 with Retry-After: the engine should retry even for POST (429 is always retryable)
    HttpTransport.HttpResponse response =
        engine.execute(
            () -> {
              int n = attempts.incrementAndGet();
              if (n <= 1) {
                throw new RetryEngine.RetryableResponseException(
                    429, 0, "TooManyRequests", "Rate limited", "req-id", "POST", "/Items");
              }
              return successResponse();
            },
            "POST",
            RetryPolicy.DEFAULT);

    assertEquals(200, response.statusCode());
    assertEquals(2, attempts.get());
  }

  // ── Connection failure tests ──────────────────────────────────────────

  @Test
  void connectionFailureRetriesForGetUpToTwoTimes() {
    RetryConfig config =
        RetryConfig.builder().maxRetries(3).initialBackoff(Duration.ofMillis(10)).build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    HttpTransport.HttpResponse response =
        engine.execute(
            () -> {
              int n = attempts.incrementAndGet();
              if (n <= 2) {
                throw new ShareFileNetworkException("Connection refused");
              }
              return successResponse();
            },
            "GET",
            RetryPolicy.DEFAULT);

    assertEquals(200, response.statusCode());
    assertEquals(3, attempts.get()); // 1 initial + 2 connection retries
  }

  @Test
  void connectionFailureNotRetriedForPost() {
    RetryConfig config =
        RetryConfig.builder().maxRetries(3).initialBackoff(Duration.ofMillis(10)).build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    assertThrows(
        ShareFileNetworkException.class,
        () ->
            engine.execute(
                () -> {
                  attempts.incrementAndGet();
                  throw new ShareFileNetworkException("Connection refused");
                },
                "POST",
                RetryPolicy.DEFAULT));

    assertEquals(1, attempts.get());
  }

  @Test
  void deleteOn500DoesNotRetryByDefault() {
    RetryConfig config =
        RetryConfig.builder().maxRetries(3).initialBackoff(Duration.ofMillis(10)).build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    assertThrows(
        ShareFileServerException.class,
        () ->
            engine.execute(
                () -> {
                  attempts.incrementAndGet();
                  throw new RetryEngine.RetryableResponseException(
                      500, -1, "InternalError", "Server error", "req-id", "DELETE", "/Items(1)");
                },
                "DELETE",
                RetryPolicy.DEFAULT));

    assertEquals(1, attempts.get());
  }

  @Test
  void connectionFailureRetriesExhausted() {
    RetryConfig config =
        RetryConfig.builder().maxRetries(3).initialBackoff(Duration.ofMillis(10)).build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    assertThrows(
        ShareFileNetworkException.class,
        () ->
            engine.execute(
                () -> {
                  attempts.incrementAndGet();
                  throw new ShareFileNetworkException("Timeout");
                },
                "GET",
                RetryPolicy.DEFAULT));

    assertEquals(3, attempts.get()); // 1 + 2 retries, then gives up
  }

  // ── Non-retryable status tests ────────────────────────────────────────

  @Test
  void status400NotRetried() {
    RetryConfig config = RetryConfig.builder().maxRetries(3).build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    assertThrows(
        ShareFileApiException.class,
        () ->
            engine.execute(
                () -> {
                  attempts.incrementAndGet();
                  throw retryable(400, "BadRequest", "Invalid");
                },
                "GET",
                RetryPolicy.DEFAULT));

    // 400 is not in retryableStatuses, so the engine should convert and throw immediately
    assertEquals(1, attempts.get());
  }

  // ── Metrics counter tests ─────────────────────────────────────────────

  @Test
  void metricsCounterIncrementedOnRetry() {
    RetryConfig config =
        RetryConfig.builder()
            .maxRetries(2)
            .initialBackoff(Duration.ofMillis(10))
            .jitterFactor(0.0)
            .build();
    RetryEngine engine = new RetryEngine(config, metricsProvider);

    AtomicInteger attempts = new AtomicInteger();
    engine.execute(
        () -> {
          int n = attempts.incrementAndGet();
          if (n <= 2) {
            throw retryable(503, "ServiceUnavailable", "Try again");
          }
          return successResponse();
        },
        "GET",
        RetryPolicy.DEFAULT);

    assertEquals(2, metricsProvider.counters.size());
    assertTrue(metricsProvider.counters.stream().allMatch("sharefile.retry"::equals));
  }

  // ── RetryConfig tests ─────────────────────────────────────────────────

  @Test
  void retryConfigDefaults() {
    RetryConfig config = RetryConfig.defaults();
    assertEquals(3, config.getMaxRetries());
    assertEquals(Duration.ofSeconds(1), config.getInitialBackoff());
    assertEquals(2.0, config.getBackoffMultiplier());
    assertEquals(0.2, config.getJitterFactor());
    assertTrue(config.isRetryOnConnectionFailure());
    assertTrue(config.getRetryableStatuses().contains(429));
    assertTrue(config.getRetryableStatuses().contains(500));
    assertTrue(config.getRetryableStatuses().contains(502));
    assertTrue(config.getRetryableStatuses().contains(503));
    assertTrue(config.getRetryableStatuses().contains(504));
  }

  @Test
  void retryConfigValidation() {
    assertThrows(IllegalArgumentException.class, () -> RetryConfig.builder().maxRetries(-1));
    assertThrows(
        IllegalArgumentException.class, () -> RetryConfig.builder().backoffMultiplier(0.5));
    assertThrows(IllegalArgumentException.class, () -> RetryConfig.builder().jitterFactor(-0.1));
    assertThrows(IllegalArgumentException.class, () -> RetryConfig.builder().jitterFactor(1.1));
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private static RetryEngine.RetryableResponseException retryable(
      int status, String code, String message) {
    return new RetryEngine.RetryableResponseException(
        status, -1, code, message, "req-id", "GET", "/test");
  }

  private static HttpTransport.HttpResponse successResponse() {
    return new HttpTransport.HttpResponse() {
      @Override
      public int statusCode() {
        return 200;
      }

      @Override
      public Map<String, List<String>> headers() {
        return Map.of();
      }

      @Override
      public InputStream bodyStream() {
        return new ByteArrayInputStream(new byte[0]);
      }

      @Override
      public byte[] bodyBytes() {
        return new byte[0];
      }

      @Override
      public byte[] bodyBytes(int maxBytes) {
        return new byte[0];
      }

      @Override
      public void close() {}
    };
  }
}
