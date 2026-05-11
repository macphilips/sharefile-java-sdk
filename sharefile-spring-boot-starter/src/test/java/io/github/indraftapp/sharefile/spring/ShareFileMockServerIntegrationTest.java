package io.github.indraftapp.sharefile.spring;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.indraftapp.sharefile.client.ShareFileClient;
import io.github.indraftapp.sharefile.spring.http.RestClientHttpTransport;
import io.github.indraftapp.sharefile.test.ShareFileMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = ShareFileMockServerIntegrationTest.TestConfig.class)
@ShareFileMockServer
class ShareFileMockServerIntegrationTest {

  @Autowired private ShareFileClient client;

  @Autowired private WireMockServer wireMockServer;

  @Test
  void shareFileClientCanTargetWireMockBaseUrl() {
    wireMockServer.stubFor(
        get(urlEqualTo("/sf/v3/Accounts"))
            .willReturn(aResponse().withStatus(200).withBody("{\"Subdomain\":\"testco\"}")));

    assertThat(client.accounts().get().getSubdomain()).isEqualTo("testco");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestConfig {
    @Bean
    ShareFileClient shareFileClient(Environment environment) {
      return ShareFileClient.builder()
          .subdomain(environment.getProperty("sharefile.subdomain"))
          .clientCredentials("client-id", "client-secret")
          .accessToken("seeded-token", "refresh-token")
          .baseUrl(environment.getProperty("sharefile.mock.base-url"))
          .httpTransport(new RestClientHttpTransport(RestClient.builder().build()))
          .build();
    }
  }
}
