package io.github.indraftapp.sharefile.spring.autoconfigure;

import io.github.indraftapp.sharefile.core.model.enums.GrantType;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Spring Boot configuration properties for ShareFile SDK integration. */
@Getter
@Setter
@ConfigurationProperties(prefix = "sharefile")
public class ShareFileProperties {

  private String subdomain;
  private String apiControlPlane = "sharefile.com";
  private Auth auth = new Auth();
  private Http http = new Http();
  private Transfer transfer = new Transfer();
  private Resilience resilience = new Resilience();

  /** Authentication-related ShareFile properties. */
  @Getter
  @Setter
  public static class Auth {
    private String clientId;
    private String clientSecret;
    private GrantType grantType = GrantType.PASSWORD;
    private String username;
    private String password;
    private String code;
    private Token token = new Token();
  }

  /** Token refresh-related ShareFile properties. */
  @Getter
  @Setter
  public static class Token {
    private int refreshBufferSeconds = 300;
  }

  /** HTTP timeout properties for ShareFile requests. */
  @Getter
  @Setter
  public static class Http {
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
    private Duration uploadReadTimeout = Duration.ofSeconds(300);
    private Duration downloadReadTimeout = Duration.ofSeconds(300);
  }

  /** Transfer executor sizing properties. */
  @Getter
  @Setter
  public static class Transfer {
    private int corePoolSize = 2;
    private int maxPoolSize = 8;
    private int queueCapacity = 50;
  }

  /** Resilience property group retained for starter binding completeness. */
  @Getter
  @Setter
  public static class Resilience {
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
  }

  /** Circuit breaker properties retained for a later ticket. */
  @Getter
  @Setter
  public static class CircuitBreaker {
    private boolean enabled;
    private int failureRateThreshold = 50;
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);
    private int slidingWindowSize = 20;
  }
}
