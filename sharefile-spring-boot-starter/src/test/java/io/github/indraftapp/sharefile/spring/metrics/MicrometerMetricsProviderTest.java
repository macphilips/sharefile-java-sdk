package io.github.indraftapp.sharefile.spring.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.indraftapp.sharefile.client.MetricNames;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MicrometerMetricsProviderTest {

  @Test
  void requestTimerIsRecordedWithExpectedTags() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerMetricsProvider provider = new MicrometerMetricsProvider(registry);

    io.github.indraftapp.sharefile.client.spi.MetricsProvider.Timer timer = provider.startTimer();
    provider.recordRequest(timer, "Items", "GET", 200);

    assertThat(
            registry
                .find(MetricNames.HTTP_REQUESTS)
                .tags("entity", "Items", "method", "GET", "status", "200")
                .timer())
        .isNotNull();
  }

  @Test
  void countersGaugesAndValuesPublishToRegistry() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerMetricsProvider provider = new MicrometerMetricsProvider(registry);

    provider.recordError("Items", "POST", 500);
    provider.incrementCounter(MetricNames.RETRY_ATTEMPTS, "entity", "Items");
    provider.recordValue(MetricNames.TRANSFER_UPLOAD_BYTES, 42.0, "entity", "Items");
    provider.setGauge(MetricNames.HTTP_REQUESTS_ACTIVE, 3.0, "entity", "Items");

    assertThat(registry.find(MetricNames.HTTP_ERRORS).counter()).isNotNull();
    assertThat(registry.find(MetricNames.RETRY_ATTEMPTS).counter()).isNotNull();
    assertThat(registry.find(MetricNames.TRANSFER_UPLOAD_BYTES).summary()).isNotNull();
    assertThat(registry.find(MetricNames.HTTP_REQUESTS_ACTIVE).gauge()).isNotNull();
    assertThat(
            registry
                .find(MetricNames.HTTP_REQUESTS_ACTIVE)
                .tags("entity", "Items")
                .gauge()
                .value())
        .isEqualTo(3.0);
  }

  @Test
  void fallbackTimerElapsedIsSupported() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerMetricsProvider provider = new MicrometerMetricsProvider(registry);

    provider.recordRequest(
        new io.github.indraftapp.sharefile.client.spi.MetricsProvider.Timer() {
          @Override
          public void stop() {}

          @Override
          public Duration elapsed() {
            return Duration.ofMillis(5);
          }
        },
        "Users",
        "GET",
        204);

    assertThat(
            registry
                .find(MetricNames.HTTP_REQUESTS)
                .tags("entity", "Users", "method", "GET", "status", "204")
                .timer()
                .count())
        .isEqualTo(1);
  }
}
