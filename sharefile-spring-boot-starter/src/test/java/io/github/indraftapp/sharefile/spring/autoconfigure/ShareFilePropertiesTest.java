package io.github.indraftapp.sharefile.spring.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ShareFilePropertiesTest {

  @Test
  void defaultsMatchSpec() {
    ShareFileProperties properties = new ShareFileProperties();

    assertThat(properties.getApiControlPlane()).isEqualTo("sharefile.com");
    assertThat(properties.getAuth().getGrantType())
        .isEqualTo(io.github.indraftapp.sharefile.core.model.enums.GrantType.PASSWORD);
    assertThat(properties.getAuth().getToken().getRefreshBufferSeconds()).isEqualTo(300);
    assertThat(properties.getHttp().getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(properties.getHttp().getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
    assertThat(properties.getHttp().getUploadReadTimeout()).isEqualTo(Duration.ofSeconds(300));
    assertThat(properties.getHttp().getDownloadReadTimeout()).isEqualTo(Duration.ofSeconds(300));
    assertThat(properties.getTransfer().getCorePoolSize()).isEqualTo(2);
    assertThat(properties.getTransfer().getMaxPoolSize()).isEqualTo(8);
    assertThat(properties.getTransfer().getQueueCapacity()).isEqualTo(50);
    assertThat(properties.getResilience().getCircuitBreaker().getFailureRateThreshold())
        .isEqualTo(50);
  }

  @Test
  void relaxedBindingSupportsKebabCaseProperties() {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfig.class)
        .withPropertyValues(
            "sharefile.subdomain=testco",
            "sharefile.auth.client-id=client-id",
            "sharefile.auth.client-secret=client-secret",
            "sharefile.auth.grant-type=authorization_code",
            "sharefile.auth.code=auth-code",
            "sharefile.http.upload-read-timeout=123s",
            "sharefile.auth.token.refresh-buffer-seconds=456")
        .run(
            context -> {
              ShareFileProperties properties = context.getBean(ShareFileProperties.class);
              assertThat(properties.getSubdomain()).isEqualTo("testco");
              assertThat(properties.getAuth().getClientId()).isEqualTo("client-id");
              assertThat(properties.getAuth().getGrantType())
                  .isEqualTo(
                      io.github.indraftapp.sharefile.core.model.enums.GrantType
                          .AUTHORIZATION_CODE);
              assertThat(properties.getAuth().getToken().getRefreshBufferSeconds()).isEqualTo(456);
              assertThat(properties.getHttp().getUploadReadTimeout())
                  .isEqualTo(Duration.ofSeconds(123));
            });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ShareFileProperties.class)
  static class PropertiesConfig {}
}
