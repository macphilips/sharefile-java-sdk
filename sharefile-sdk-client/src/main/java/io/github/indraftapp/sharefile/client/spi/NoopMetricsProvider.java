package io.github.indraftapp.sharefile.client.spi;

import java.time.Duration;
import java.time.Instant;

/** A no-op {@link MetricsProvider} that discards all metrics. */
final class NoopMetricsProvider implements MetricsProvider {

  static final NoopMetricsProvider INSTANCE = new NoopMetricsProvider();

  private NoopMetricsProvider() {}

  @Override
  public Timer startTimer() {
    return new NoopTimer();
  }

  @Override
  public void recordRequest(Timer timer, String entity, String method, int status) {}

  @Override
  public void recordError(String entity, String method, int status) {}

  @Override
  public void incrementCounter(String name, String... tags) {}

  @Override
  public void recordValue(String name, double value, String... tags) {}

  @Override
  public void setGauge(String name, double value, String... tags) {}

  /** Timer that still tracks elapsed time (useful for error context) but records nothing. */
  private static final class NoopTimer implements Timer {
    private final Instant start = Instant.now();

    @Override
    public void stop() {}

    @Override
    public Duration elapsed() {
      return Duration.between(start, Instant.now());
    }
  }
}
