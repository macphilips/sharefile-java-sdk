package io.github.indraftapp.sharefile.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ShareFileMockServerConfiguration {

  @Bean
  WireMockServer wireMockServer() {
    return ShareFileMockServerRegistry.getRequired();
  }
}
