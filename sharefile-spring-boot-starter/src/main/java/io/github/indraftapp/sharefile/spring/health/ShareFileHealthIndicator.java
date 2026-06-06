package io.github.indraftapp.sharefile.spring.health;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import io.github.indraftapp.sharefile.client.ShareFileClient;
import io.github.indraftapp.sharefile.core.model.HealthStatus;
import java.time.Instant;
import java.time.ZoneId;
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
        String formattedExpiry = formatRemainingDuration(status.tokenExpiresInSeconds());
        log.info("[ShareFileHealthIndicator] share health status is {}, token expires in {} mins. Error {}",
                status.up() ? "UP" : "DOWN",
                formattedExpiry,
                status.error() != null ? status.error() : "No error");
        Health.Builder builder = status.up() ? Health.up() : Health.down();
        if (status.subdomain() != null) {
            builder.withDetail("subdomain", status.subdomain());
        }
        if (status.tokenExpiresInSeconds() != null) {
            var iso = RFC_1123_DATE_TIME.format(
                    Instant.now()
                            .atZone(ZoneId.of("UTC"))
                            .plusSeconds(status.tokenExpiresInSeconds())
            );
            builder.withDetail("tokenExpiresIn", formattedExpiry);
            builder.withDetail("tokenExpiration", "%s".formatted(iso));
        }
        if (status.error() != null) {
            builder.withDetail("error", status.error());
        }
        return builder.build();
    }

    private static String formatRemainingDuration(Long secondsRemaining) {
        if (secondsRemaining == null) {
            return null;
        }

        long totalSeconds = Math.max(0L, secondsRemaining);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return "%dh %dm %ds".formatted(hours, minutes, seconds);
        }
        if (minutes > 0) {
            return "%dm %ds".formatted(minutes, seconds);
        }
        return "%ds".formatted(seconds);
    }
}
