package me.dhiren9939.mint.common;

import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisHealthIndicatorTest {

    @Mock
    private RedisConnectionProvider redisConnectionProvider;

    @InjectMocks
    private RedisHealthIndicator redisHealthIndicator;

    @Test
    @DisplayName("health: UP and connected when the shared connection is open")
    void health_reportsConnected() {
        @SuppressWarnings("unchecked")
        StatefulRedisConnection<String, byte[]> connection = mock(StatefulRedisConnection.class);
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(connection));
        when(redisConnectionProvider.getHost()).thenReturn("cache.local");

        Health health = redisHealthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(true, health.getDetails().get("connected"));
        assertEquals("cache.local", health.getDetails().get("host"));
    }

    @Test
    @DisplayName("health: still UP but not connected when Redis is unavailable")
    void health_reportsDisconnectedAsUp() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.empty());
        when(redisConnectionProvider.getHost()).thenReturn("cache.local");

        Health health = redisHealthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(false, health.getDetails().get("connected"));
    }
}
