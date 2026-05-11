package io.github.indraftapp.sharefile.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;

final class ShareFileMockServerInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final String PROPERTY_SOURCE = "sharefileMockServer";

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    WireMockServer wireMockServer =
        new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMockServer.start();
    ShareFileMockServerRegistry.set(wireMockServer);

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("sharefile.subdomain", "testco");
    properties.put("sharefile.auth.client-id", "client-id");
    properties.put("sharefile.auth.client-secret", "client-secret");
    properties.put("sharefile.auth.grant-type", "authorization_code");
    properties.put("sharefile.auth.code", "auth-code");
    properties.put("sharefile.mock.base-url", wireMockServer.baseUrl() + "/sf/v3");
    applicationContext
        .getEnvironment()
        .getPropertySources()
        .addFirst(new MapPropertySource(PROPERTY_SOURCE, properties));

    applicationContext.addApplicationListener(
        (ApplicationListener<ContextClosedEvent>)
            event -> {
              wireMockServer.stop();
              ShareFileMockServerRegistry.clear();
            });
  }
}
