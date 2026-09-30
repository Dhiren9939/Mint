package me.dhiren9939.mint.common;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import me.dhiren9939.mint.common.RedisRateLimiter.Limit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the real script, so it needs a Redis (REDIS_TEST_HOST, default localhost:6379). Skipped when there is none.
 */
class RedisRateLimiterTest {

    private RedisClient client;
    private StatefulRedisConnection<String, byte[]> connection;
    private RedisRateLimiter limiter;
    private String id;

    @BeforeEach
    void setUp() {
        String host = System.getenv().getOrDefault("REDIS_TEST_HOST", "localhost");
        try {
            client = RedisClient.create("redis://" + host + ":6379");
            connection = client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        } catch (RuntimeException e) {
            connection = null;
        }
        assumeTrue(connection != null, "no redis to test against");
        limiter = new RedisRateLimiter();
        id = UUID.randomUUID().toString();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.sync().del("rl:G" + id, "rl:I" + id, "rl:U" + id);
            connection.close();
            client.shutdown();
        }
    }

    private Limit global(int capacity, Duration window) {
        return new Limit("G" + id, capacity, window);
    }

    private Limit ip(int capacity, Duration window) {
        return new Limit("I" + id, capacity, window);
    }

    private Limit user(int capacity, Duration window) {
        return new Limit("U" + id, capacity, window);
    }

    private static final Duration LONG = Duration.ofDays(1);

    @Test
    @DisplayName("allows up to the capacity, then names the bucket that ran out")
    void allowsThenRejects() {
        for (int i = 0; i < 3; i++) {
            assertNull(limiter.check(connection, global(100, LONG), ip(100, LONG), user(3, LONG)));
        }
        assertEquals("User", limiter.check(connection, global(100, LONG), ip(100, LONG), user(3, LONG)));
    }

    @Test
    @DisplayName("checks global first, then ip, then user")
    void reportsFirstFailingBucket() {
        assertNull(limiter.check(connection, global(1, LONG), ip(1, LONG), user(1, LONG)));
        assertEquals("Global", limiter.check(connection, global(1, LONG), ip(1, LONG), user(1, LONG)));
    }

    @Test
    @DisplayName("a rejection takes nothing from the other buckets")
    void rejectionIsAllOrNothing() {
        assertNull(limiter.check(connection, global(10, LONG), ip(10, LONG), user(1, LONG)));
        for (int i = 0; i < 5; i++) {
            assertEquals("User", limiter.check(connection, global(10, LONG), ip(10, LONG), user(1, LONG)));
        }
        // global and ip were only used once, so 9 are left (with a different user, whose own bucket is untouched)
        Limit other = new Limit("V" + id, 100, LONG);
        for (int i = 0; i < 9; i++) {
            assertNull(limiter.check(connection, global(10, LONG), ip(10, LONG), other));
        }
        assertEquals("Global", limiter.check(connection, global(10, LONG), ip(10, LONG), other));
        connection.sync().del("rl:V" + id);
    }

    @Test
    @DisplayName("tokens come back over the window")
    void refills() throws Exception {
        Duration window = Duration.ofMillis(400);
        assertNull(limiter.check(connection, global(100, LONG), ip(1, window), user(100, LONG)));
        assertEquals("IP", limiter.check(connection, global(100, LONG), ip(1, window), user(100, LONG)));
        Thread.sleep(500);
        assertNull(limiter.check(connection, global(100, LONG), ip(1, window), user(100, LONG)));
    }

    @Test
    @DisplayName("keys expire once the bucket would be full again")
    void keysExpire() {
        limiter.check(connection, global(10, Duration.ofMinutes(1)), ip(10, LONG), user(10, LONG));
        long ttl = connection.sync().pttl("rl:G" + id);
        assertTrue(ttl > 0 && ttl <= Duration.ofMinutes(1).toMillis() / 10 + 1000, "ttl was " + ttl);
    }

    @Test
    @DisplayName("many threads on the same keys never let more than the capacity through")
    void concurrentCallersOnHotKeys() throws Exception {
        int capacity = 50;
        AtomicInteger allowed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(64);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            futures.add(pool.submit(() -> {
                if (limiter.check(connection, global(capacity, LONG), ip(1000, LONG), user(1000, LONG)) == null) {
                    allowed.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        assertEquals(capacity, allowed.get());
    }
}
