package io.github.indraftapp.sharefile.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@ShareFileMockServer
class ShareFileMockServerTest {

  @Autowired private WireMockServer wireMockServer;

  @Autowired private Environment environment;

  @Test
  void startsWireMockAndRegistersTestProperties() {
    assertThat(wireMockServer.isRunning()).isTrue();
    assertThat(environment.getProperty("sharefile.subdomain")).isEqualTo("testco");
    assertThat(environment.getProperty("sharefile.mock.base-url"))
        .isEqualTo(wireMockServer.baseUrl() + "/sf/v3");
  }
}
