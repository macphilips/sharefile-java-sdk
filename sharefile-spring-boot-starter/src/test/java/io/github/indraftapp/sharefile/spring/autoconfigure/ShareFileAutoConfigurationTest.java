package io.github.indraftapp.sharefile.spring.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.indraftapp.sharefile.client.ShareFileClient;
import io.github.indraftapp.sharefile.client.spi.CredentialProvider;
import io.github.indraftapp.sharefile.client.spi.Credentials;
import io.github.indraftapp.sharefile.spring.health.ShareFileHealthIndicator;
import io.github.indraftapp.sharefile.spring.http.RestClientHttpTransport;
import io.github.indraftapp.sharefile.spring.metrics.MicrometerMetricsProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

class ShareFileAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ShareFileAutoConfiguration.class));

  @Test
  void propertiesCreateShareFileClientBean() {
    contextRunner
        .withPropertyValues(
            "sharefile.subdomain=testco",
            "sharefile.auth.client-id=client-id",
            "sharefile.auth.client-secret=client-secret",
            "sharefile.auth.grant-type=authorization_code",
            "sharefile.auth.code=auth-code")
        .run(
            context -> {
              assertThat(context).hasSingleBean(ShareFileClient.class);
              assertThat(context).hasSingleBean(ShareFileHealthIndicator.class);
            });
  }

  @Test
  void customShareFileClientSuppressesAutoConfiguration() {
    contextRunner
        .withUserConfiguration(CustomClientConfiguration.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(ShareFileClient.class);
              assertThat(context.getBean(ShareFileClient.class))
                  .isSameAs(context.getBean("customShareFileClient"));
            });
  }

  @Test
  void credentialProviderBeanOverridesPropertiesAuth() {
    contextRunner
        .withUserConfiguration(CredentialProviderConfiguration.class)
        .withPropertyValues("sharefile.subdomain=testco")
        .run(
            context -> {
              assertThat(context).hasSingleBean(ShareFileClient.class);
              assertThat(context).hasSingleBean(CredentialProvider.class);
            });
  }

  @Test
  void meterRegistryInstallsMicrometerMetricsProvider() {
    contextRunner
        .withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
        .withPropertyValues(
            "sharefile.subdomain=testco",
            "sharefile.auth.client-id=client-id",
            "sharefile.auth.client-secret=client-secret",
            "sharefile.auth.grant-type=authorization_code",
            "sharefile.auth.code=auth-code")
        .run(
            context -> {
              ShareFileClient client = context.getBean(ShareFileClient.class);
              Object metrics =
                  readField(
                      readField(readField(client.items(), "executor"), "httpClient"), "metrics");
              assertThat(metrics).isInstanceOf(MicrometerMetricsProvider.class);
            });
  }

  @Test
  void restClientBuilderInstallsRestClientTransport() {
    contextRunner
        .withBean(RestClient.Builder.class, RestClient::builder)
        .withPropertyValues(
            "sharefile.subdomain=testco",
            "sharefile.auth.client-id=client-id",
            "sharefile.auth.client-secret=client-secret",
            "sharefile.auth.grant-type=authorization_code",
            "sharefile.auth.code=auth-code")
        .run(
            context -> {
              ShareFileClient client = context.getBean(ShareFileClient.class);
              assertThat(readField(client, "transport")).isInstanceOf(RestClientHttpTransport.class);
            });
  }

  @Test
  void transferExecutorUsesConfiguredSizing() {
    contextRunner
        .withPropertyValues(
            "sharefile.subdomain=testco",
            "sharefile.auth.client-id=client-id",
            "sharefile.auth.client-secret=client-secret",
            "sharefile.auth.grant-type=authorization_code",
            "sharefile.auth.code=auth-code",
            "sharefile.transfer.core-pool-size=3",
            "sharefile.transfer.max-pool-size=9",
            "sharefile.transfer.queue-capacity=77")
        .run(
            context -> {
              ExecutorService executor =
                  (ExecutorService) context.getBean("shareFileTransferExecutor");
              ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
              assertThat(threadPool.getCorePoolSize()).isEqualTo(3);
              assertThat(threadPool.getMaximumPoolSize()).isEqualTo(9);
              assertThat(threadPool.getQueue().remainingCapacity()).isEqualTo(77);

              ShareFileClient client = context.getBean(ShareFileClient.class);
              assertThat(readField(client, "executor")).isSameAs(executor);
            });
  }

  @Test
  void healthIndicatorCanBeDisabled() {
    contextRunner
        .withPropertyValues(
            "sharefile.subdomain=testco",
            "sharefile.auth.client-id=client-id",
            "sharefile.auth.client-secret=client-secret",
            "sharefile.auth.grant-type=authorization_code",
            "sharefile.auth.code=auth-code",
            "sharefile.health.enabled=false")
        .run(
            context -> {
              assertThat(context).hasSingleBean(ShareFileClient.class);
              assertThat(context).doesNotHaveBean(HealthIndicator.class);
              assertThat(context).doesNotHaveBean(ShareFileHealthIndicator.class);
            });
  }

  @Test
  void autoConfiguredClientExposesRuntimeReauthenticationApi() {
    contextRunner
        .withPropertyValues(
            "sharefile.subdomain=testco",
            "sharefile.auth.client-id=client-id",
            "sharefile.auth.client-secret=client-secret",
            "sharefile.auth.grant-type=authorization_code",
            "sharefile.auth.code=auth-code")
        .run(
            context -> {
              ShareFileClient client = context.getBean(ShareFileClient.class);
              assertThat(client).isNotNull();
              assertThat(findMethod("reauthenticate")).isNotNull();
              assertThat(findMethod("reauthenticate", Credentials.class)).isNotNull();
              assertThat(findMethod("reauthenticate", CredentialProvider.class)).isNotNull();
            });
  }

  private static java.lang.reflect.Method findMethod(String name, Class<?>... parameterTypes) {
    try {
      return ShareFileClient.class.getMethod(name, parameterTypes);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Missing method " + name, e);
    }
  }

  private static Object readField(Object target, String name) {
    try {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to read field " + name, e);
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomClientConfiguration {
    @Bean("customShareFileClient")
    ShareFileClient customShareFileClient() {
      return ShareFileClient.builder()
          .subdomain("custom")
          .clientCredentials("client-id", "client-secret")
          .authorizationCode("auth-code")
          .httpTransport(request -> {
            throw new UnsupportedOperationException("No requests expected");
          })
          .build();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CredentialProviderConfiguration {
    @Bean
    CredentialProvider shareFileCredentialProvider() {
      return () ->
          Credentials.builder()
              .clientCredentials("client-id", "client-secret")
              .passwordGrant("user@example.com", "password")
              .build();
    }
  }
}
