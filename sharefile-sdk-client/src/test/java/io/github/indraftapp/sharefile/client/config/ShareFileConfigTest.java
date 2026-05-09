package io.github.indraftapp.sharefile.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** SF-06: Tests for {@link ShareFileConfig}. */
class ShareFileConfigTest {

  @Test
  void builder_withDefaults() {
    var config = ShareFileConfig.builder().subdomain("mycompany").build();

    assertThat(config.getSubdomain()).isEqualTo("mycompany");
    assertThat(config.getApiControlPlane()).isEqualTo("sharefile.com");
    assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
    assertThat(config.getUploadTimeout()).isEqualTo(Duration.ofSeconds(300));
    assertThat(config.getDownloadTimeout()).isEqualTo(Duration.ofSeconds(300));
    assertThat(config.getTokenRefreshBuffer()).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void builder_customValues() {
    var config =
        ShareFileConfig.builder()
            .subdomain("acme")
            .apiControlPlane("sharefile.eu")
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(60))
            .uploadTimeout(Duration.ofSeconds(600))
            .downloadTimeout(Duration.ofSeconds(600))
            .tokenRefreshBuffer(Duration.ofMinutes(10))
            .build();

    assertThat(config.getSubdomain()).isEqualTo("acme");
    assertThat(config.getApiControlPlane()).isEqualTo("sharefile.eu");
    assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(15));
    assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(60));
    assertThat(config.getUploadTimeout()).isEqualTo(Duration.ofSeconds(600));
    assertThat(config.getDownloadTimeout()).isEqualTo(Duration.ofSeconds(600));
    assertThat(config.getTokenRefreshBuffer()).isEqualTo(Duration.ofMinutes(10));
  }

  @Test
  void baseUrl_computedFromSubdomain() {
    var config = ShareFileConfig.builder().subdomain("mycompany").build();

    assertThat(config.getBaseUrl()).isEqualTo("https://mycompany.sf-api.com/sf/v3");
  }

  @Test
  void tokenEndpointUrl_computedFromSubdomainAndControlPlane() {
    var config = ShareFileConfig.builder().subdomain("mycompany").build();

    assertThat(config.getTokenEndpointUrl())
        .isEqualTo("https://mycompany.sharefile.com/oauth/token");
  }

  @Test
  void tokenEndpointUrl_customControlPlane() {
    var config =
        ShareFileConfig.builder().subdomain("acme").apiControlPlane("sharefile.eu").build();

    assertThat(config.getTokenEndpointUrl()).isEqualTo("https://acme.sharefile.eu/oauth/token");
  }

  @Test
  void builder_missingSubdomain_throws() {
    assertThatThrownBy(() -> ShareFileConfig.builder().build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("subdomain");
  }

  @Test
  void builder_nullSubdomain_throws() {
    assertThatThrownBy(() -> ShareFileConfig.builder().subdomain(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void builder_nullTimeout_throws() {
    assertThatThrownBy(() -> ShareFileConfig.builder().connectTimeout(null))
        .isInstanceOf(NullPointerException.class);
  }
}
