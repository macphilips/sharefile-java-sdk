package io.github.indraftapp.sharefile.client.internal;

import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Test metrics provider that records all calls for verification. */
final class RecordingMetricsProvider implements MetricsProvider {

  boolean requestRecorded;
  boolean errorRecorded;
  final List<String> counters = new ArrayList<>();

  @Override
  public Timer startTimer() {
    return new RecordingTimer();
  }

  @Override
  public void recordRequest(Timer timer, String entity, String method, int status) {
    requestRecorded = true;
  }

  @Override
  public void recordError(String entity, String method, int status) {
    errorRecorded = true;
  }

  @Override
  public void incrementCounter(String name, String... tags) {
    counters.add(name);
  }

  @Override
  public void recordValue(String name, double value, String... tags) {}

  @Override
  public void setGauge(String name, double value, String... tags) {}

  private static final class RecordingTimer implements Timer {
    private final Instant start = Instant.now();

    @Override
    public void stop() {}

    @Override
    public Duration elapsed() {
      return Duration.between(start, Instant.now());
    }
  }
}
