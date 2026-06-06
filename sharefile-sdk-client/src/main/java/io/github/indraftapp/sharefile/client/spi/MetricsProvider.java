package io.github.indraftapp.sharefile.client.spi;

import java.time.Duration;

/**
 * SPI for recording SDK metrics (request latency, error counts, retries).
 *
 * <p>The default implementation is a no-op. The Spring Boot starter provides a Micrometer-backed
 * implementation.
 */
public interface MetricsProvider {

  /** Starts a timer for measuring request latency. */
  Timer startTimer();

  /**
   * Records a completed request.
   *
   * @param timer the timer started before the request
   * @param entity the resource entity name (e.g. "Items", "Users")
   * @param method the HTTP method
   * @param status the HTTP response status code
   */
  void recordRequest(Timer timer, String entity, String method, int status);

  /**
   * Records an error.
   *
   * @param entity the resource entity name
   * @param method the HTTP method
   * @param status the HTTP response status code
   */
  void recordError(String entity, String method, int status);

  /**
   * Increments a named counter.
   *
   * @param name the counter name
   * @param tags key-value pairs for dimensional tagging
   */
  void incrementCounter(String name, String... tags);

  /**
   * Records a gauge-style value.
   *
   * @param name the metric name
   * @param value the value to record
   * @param tags key-value pairs for dimensional tagging
   */
  void recordValue(String name, double value, String... tags);

  /**
   * Sets a gauge value.
   *
   * @param name the gauge name
   * @param value the current value
   * @param tags key-value pairs for dimensional tagging
   */
  void setGauge(String name, double value, String... tags);

  /** Returns a no-op metrics provider that discards all metrics. */
  static MetricsProvider noop() {
    return NoopMetricsProvider.INSTANCE;
  }

  /** A timer for measuring elapsed time. */
  interface Timer {
    /** Stops the timer. */
    void stop();

    /** Returns the elapsed duration since the timer was started. */
    Duration elapsed();
  }
}
