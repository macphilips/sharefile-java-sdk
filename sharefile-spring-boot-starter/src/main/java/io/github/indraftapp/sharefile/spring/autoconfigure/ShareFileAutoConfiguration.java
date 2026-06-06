package io.github.indraftapp.sharefile.spring.autoconfigure;

import io.github.indraftapp.sharefile.client.ShareFileClient;
import io.github.indraftapp.sharefile.client.ShareFileClientBuilder;
import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.spring.health.ShareFileHealthIndicator;
import io.github.indraftapp.sharefile.spring.http.RestClientHttpTransport;
import io.github.indraftapp.sharefile.spring.metrics.MicrometerMetricsProvider;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Auto-configuration for ShareFile SDK Spring Boot integration. */
@AutoConfiguration
@ConditionalOnClass(ShareFileClient.class)
@EnableConfigurationProperties(ShareFileProperties.class)
public class ShareFileAutoConfiguration {

  @Bean(name = "shareFileTransferExecutor", destroyMethod = "shutdown")
  @ConditionalOnMissingBean(name = "shareFileTransferExecutor")
  public ExecutorService shareFileTransferExecutor(ShareFileProperties properties) {
    ShareFileProperties.Transfer transfer = properties.getTransfer();
    return new ThreadPoolExecutor(
        transfer.getCorePoolSize(),
        transfer.getMaxPoolSize(),
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(transfer.getQueueCapacity()),
        runnable -> {
          Thread thread = new Thread(runnable, "sharefile-spring-transfer");
          thread.setDaemon(true);
          return thread;
        });
  }

  @Bean
  @ConditionalOnMissingBean
  public ShareFileClient shareFileClient(
      ShareFileProperties properties,
      Optional<CredentialProvider> credentialProvider,
      Optional<MeterRegistry> meterRegistry,
      Optional<RestClient.Builder> restClientBuilder,
      @Qualifier("shareFileTransferExecutor") ExecutorService transferExecutor) {
    ShareFileClientBuilder builder =
        ShareFileClient.builder()
            .subdomain(properties.getSubdomain())
            .apiControlPlane(properties.getApiControlPlane())
            .connectTimeout(properties.getHttp().getConnectTimeout())
            .readTimeout(properties.getHttp().getReadTimeout())
            .uploadTimeout(properties.getHttp().getUploadReadTimeout())
            .downloadTimeout(properties.getHttp().getDownloadReadTimeout())
            .tokenRefreshBuffer(
                Duration.ofSeconds(properties.getAuth().getToken().getRefreshBufferSeconds()))
            .executor(transferExecutor);

    if (credentialProvider.isPresent()) {
      builder.credentialProvider(credentialProvider.get());
    } else {
      builder.clientCredentials(
          properties.getAuth().getClientId(), properties.getAuth().getClientSecret());
      switch (properties.getAuth().getGrantType()) {
        case PASSWORD ->
            builder.passwordGrant(
                properties.getAuth().getUsername(), properties.getAuth().getPassword());
        case AUTHORIZATION_CODE -> builder.authorizationCode(properties.getAuth().getCode());
        default ->
            throw new IllegalStateException(
                "Unsupported sharefile.auth.grant-type for Spring properties: "
                    + properties.getAuth().getGrantType());
      }
    }

    meterRegistry.ifPresent(
        registry -> builder.metricsProvider(new MicrometerMetricsProvider(registry)));

    restClientBuilder.ifPresent(
        builderBean ->
            builder.httpTransport(
                new RestClientHttpTransport(
                    builderBean.requestFactory(new JdkClientHttpRequestFactory()).build())));

    return builder.build();
  }

  @Bean
  @ConditionalOnClass(HealthIndicator.class)
  @ConditionalOnBean(ShareFileClient.class)
  @ConditionalOnProperty(
      prefix = "sharefile.health",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public ShareFileHealthIndicator shareFileHealthIndicator(ShareFileClient client) {
    return new ShareFileHealthIndicator(client);
  }
}
