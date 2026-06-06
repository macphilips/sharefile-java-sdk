package io.github.indraftapp.sharefile.spring.metrics;

import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/** Micrometer-backed {@link MetricsProvider} for the Spring Boot starter. */
public final class MicrometerMetricsProvider implements MetricsProvider {

  private final MeterRegistry registry;
  private final ConcurrentMap<GaugeKey, AtomicReference<Double>> gauges = new ConcurrentHashMap<>();

  public MicrometerMetricsProvider(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry must not be null");
  }

  @Override
  public Timer startTimer() {
    return new MicrometerTimer(io.micrometer.core.instrument.Timer.start(registry));
  }

  @Override
  public void recordRequest(Timer timer, String entity, String method, int status) {
    io.micrometer.core.instrument.Timer targetTimer =
        io.micrometer.core.instrument.Timer.builder("sharefile.http.requests")
            .tags("entity", entity, "method", method, "status", String.valueOf(status))
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry);

    if (timer instanceof MicrometerTimer micrometerTimer) {
      micrometerTimer.stop(targetTimer);
    } else {
      targetTimer.record(timer.elapsed());
    }
  }

  @Override
  public void recordError(String entity, String method, int status) {
    incrementCounter("sharefile.http.errors", "entity", entity, "method", method, "status",
        String.valueOf(status));
  }

  @Override
  public void incrementCounter(String name, String... tags) {
    Counter.builder(name).tags(Tags.of(tags)).register(registry).increment();
  }

  @Override
  public void recordValue(String name, double value, String... tags) {
    DistributionSummary.builder(name).tags(Tags.of(tags)).register(registry).record(value);
  }

  @Override
  public void setGauge(String name, double value, String... tags) {
    GaugeKey key = new GaugeKey(name, tags.clone());
    AtomicReference<Double> ref =
        gauges.computeIfAbsent(
            key,
            gaugeKey -> {
              AtomicReference<Double> created = new AtomicReference<>(value);
              Gauge.builder(name, created, AtomicReference::get)
                  .tags(Tags.of(tags))
                  .register(registry);
              return created;
            });
    ref.set(value);
  }

  private static final class MicrometerTimer implements Timer {
    private final io.micrometer.core.instrument.Timer.Sample sample;
    private final long startedAtNanos = System.nanoTime();

    private MicrometerTimer(io.micrometer.core.instrument.Timer.Sample sample) {
      this.sample = sample;
    }

    @Override
    public void stop() {
      // The actual target timer is selected in recordRequest(...).
    }

    @Override
    public Duration elapsed() {
      return Duration.ofNanos(System.nanoTime() - startedAtNanos);
    }

    private void stop(io.micrometer.core.instrument.Timer timer) {
      sample.stop(timer);
    }
  }

  private record GaugeKey(String name, String[] tags) {
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof GaugeKey gaugeKey)) {
        return false;
      }
      return name.equals(gaugeKey.name) && Arrays.equals(tags, gaugeKey.tags);
    }

    @Override
    public int hashCode() {
      return 31 * name.hashCode() + Arrays.hashCode(tags);
    }
  }
}
