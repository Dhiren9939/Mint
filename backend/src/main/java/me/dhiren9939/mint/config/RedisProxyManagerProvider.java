package me.dhiren9939.mint.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Connects to Redis in the background so an unreachable cache never blocks startup.
 * Failed attempts are retried with exponential backoff (2s, 4s, 8s, ... capped) plus jitter.
 * {@link #get()} is empty whenever there is no open connection, and callers skip rate limiting.
 * Once connected, Lettuce's auto reconnect handles later drops such as a failover.
 */
@Slf4j
public class RedisProxyManagerProvider implements AutoCloseable {

    private static final long BASE_DELAY_MS = 2_000;
    private static final long MAX_DELAY_MS = 30_000;

    private final RedisClient client;
    private final RedisURI uri;
    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "redis-connect-retry");
        thread.setDaemon(true);
        return thread;
    });

    private volatile ProxyManager<String> proxyManager;
    private volatile StatefulRedisConnection<String, byte[]> connection;
    private volatile boolean closed;
    private int attempt;

    public RedisProxyManagerProvider(RedisClient client, RedisURI uri) {
        this.client = client;
        this.uri = uri;
        connect();
    }

    public Optional<ProxyManager<String>> get() {
        StatefulRedisConnection<String, byte[]> conn = connection;
        if (conn == null || !conn.isOpen()) {
            return Optional.empty();
        }
        return Optional.ofNullable(proxyManager);
    }

    private void connect() {
        if (closed) {
            return;
        }

        try {
            client.connectAsync(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE), uri)
                    .whenComplete((conn, error) -> {
                        if (error != null) {
                            retryLater(error);
                            return;
                        }
                        if (closed) {
                            conn.closeAsync();
                            return;
                        }
                        attempt = 0;
                        proxyManager = Bucket4jLettuce
                                .casBasedBuilder(conn)
                                .expirationAfterWrite(ExpirationAfterWriteStrategy
                                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1)))
                                .build();
                        connection = conn;
                        log.info("Connected to Redis for rate limiting");
                    });
        } catch (RuntimeException e) {
            retryLater(e);
        }
    }

    private void retryLater(Throwable error) {
        if (closed) {
            return;
        }
        long delay = Math.min(MAX_DELAY_MS, BASE_DELAY_MS << Math.min(attempt, 10));
        attempt++;
        long jitter = ThreadLocalRandom.current().nextLong(delay / 2 + 1);
        long wait = delay + jitter;

        log.warn("Could not connect to Redis (attempt {}), retrying in {}ms: {}", attempt, wait, error.toString());
        try {
            retryScheduler.schedule(this::connect, wait, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {
            // shutting down
        }
    }

    @Override
    public void close() {
        closed = true;
        retryScheduler.shutdownNow();
        StatefulRedisConnection<String, byte[]> current = connection;
        if (current != null) {
            current.close();
        }
    }
}
