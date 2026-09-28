package me.dhiren9939.mint.common;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports the state of the shared Redis connection without opening one of its own.
 * Always UP: the app fails open without Redis, so a lost connection is a degraded
 * but expected state and must never fail the health check or get tasks replaced.
 */
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionProvider redisConnectionProvider;

    @Override
    public Health health() {
        return Health.up()
                .withDetail("connected", redisConnectionProvider.getConnection().isPresent())
                .withDetail("host", redisConnectionProvider.getHost())
                .build();
    }
}
