package io.github.indraftapp.sharefile.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.concurrent.atomic.AtomicReference;

final class ShareFileMockServerRegistry {

  private static final AtomicReference<WireMockServer> SERVER = new AtomicReference<>();

  private ShareFileMockServerRegistry() {}

  static void set(WireMockServer wireMockServer) {
    SERVER.set(wireMockServer);
  }

  static WireMockServer getRequired() {
    WireMockServer wireMockServer = SERVER.get();
    if (wireMockServer == null) {
      throw new IllegalStateException("WireMockServer has not been initialized");
    }
    return wireMockServer;
  }

  static void clear() {
    SERVER.set(null);
  }
}
