package io.github.indraftapp.sharefile.client.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.indraftapp.sharefile.client.MetricNames;
import io.github.indraftapp.sharefile.client.http.HttpTransport;
import io.github.indraftapp.sharefile.client.retry.RetryConfig;
import io.github.indraftapp.sharefile.client.retry.RetryPolicy;
import io.github.indraftapp.sharefile.client.spi.MetricsProvider;
import io.github.indraftapp.sharefile.core.exception.ShareFileNetworkException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ObservabilityTest {

  @Test
  void metricNamesStayStable() {
    assertEquals("sharefile.http.requests", MetricNames.HTTP_REQUESTS);
    assertEquals("sharefile.retry.attempts", MetricNames.RETRY_ATTEMPTS);
    assertEquals("sharefile.transfer.download.duration", MetricNames.TRANSFER_DOWNLOAD_DURATION);
  }

  @Test
  void noopMetricsProviderIsSingleton() {
    assertSame(MetricsProvider.noop(), MetricsProvider.noop());
  }

  @Test
  void retryEngineEmitsRetryMetric() {
    RecordingMetricsProvider metrics = new RecordingMetricsProvider();
    RetryEngine engine =
        new RetryEngine(
            RetryConfig.builder().maxRetries(1).retryOnConnectionFailure(true).build(), metrics);
    AtomicInteger attempts = new AtomicInteger();

    engine.execute(
        () -> {
          if (attempts.getAndIncrement() == 0) {
            throw new ShareFileNetworkException("boom");
          }
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
        },
        "GET",
        RetryPolicy.DEFAULT);

    assertTrue(metrics.counters.contains(MetricNames.RETRY_ATTEMPTS));
  }

  @Test
  void logSanitizerRedactsSensitiveHeadersAndBodies() {
    Map<String, String> headers =
        LogSanitizer.redactHeaders(
            Map.of("Authorization", "Bearer secret", "Accept", "application/json"));
    String body =
        LogSanitizer.redactBody(
            "{\"access_token\":\"abc\",\"password\":\"pw\"}&client_secret=top-secret&safe=value");

    assertEquals("***", headers.get("Authorization"));
    assertEquals("application/json", headers.get("Accept"));
    assertTrue(body.contains("\"access_token\":\"***\""));
    assertTrue(body.contains("\"password\":\"***\""));
    assertTrue(body.contains("client_secret=***"));
  }
}
