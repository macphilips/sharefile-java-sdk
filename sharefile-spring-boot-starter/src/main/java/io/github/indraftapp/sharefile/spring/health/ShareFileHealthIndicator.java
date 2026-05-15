package io.github.indraftapp.sharefile.spring.health;

import io.github.indraftapp.sharefile.client.ShareFileClient;
import io.github.indraftapp.sharefile.core.model.HealthStatus;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Actuator {@link HealthIndicator} backed by {@link ShareFileClient#checkHealth()}.
 */
@Slf4j
public final class ShareFileHealthIndicator implements HealthIndicator {

    private final ShareFileClient client;

    public ShareFileHealthIndicator(ShareFileClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public Health health() {
        HealthStatus status = client.checkHealth();
        log.info("[ShareFileHealthIndicator] share health status is {}, token expires in {} mins. Error {}",
                status.up() ? "UP" : "DOWN",
                status.tokenExpiresInSeconds() != null ? (status.tokenExpiresInSeconds() / 60) : null,
                status.error() != null ? status.error() : "No error");
        Health.Builder builder = status.up() ? Health.up() : Health.down();
        if (status.subdomain() != null) {
            builder.withDetail("subdomain", status.subdomain());
        }
        if (status.tokenExpiresInSeconds() != null) {
            builder.withDetail("tokenExpiresIn", "%ds".formatted(status.tokenExpiresInSeconds()));
        }
        if (status.error() != null) {
            builder.withDetail("error", status.error());
        }
        return builder.build();
    }
}
