package me.dhiren9939.mint.common;

import io.lettuce.core.RedisNoScriptException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Global, IP and user token buckets checked in one Redis call (see ratelimit.lua).
 * The script runs atomically inside Redis, so there are no retries however many requests hit the same key.
 */
public class RedisRateLimiter {

    // Keeps these keys apart from the old Bucket4j ones, which are stored in a different format
    private static final String KEY_PREFIX = "rl:";
    private static final String[] NAMES = {"Global", "IP", "User"};

    public record Limit(String key, int capacity, Duration window) {
    }

    private final byte[] script;
    private final String sha;

    @SneakyThrows
    public RedisRateLimiter() {
        try (InputStream in = getClass().getResourceAsStream("/ratelimit.lua")) {
            script = in.readAllBytes();
        }
        sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(script));
    }

    /**
     * Takes one token from each bucket, or from none of them.
     *
     * @return the name of the bucket that rejected the request (Global, IP or User), or null if it is allowed
     */
    public String check(StatefulRedisConnection<String, byte[]> connection, Limit global, Limit ip, Limit user) {
        String[] keys = {KEY_PREFIX + global.key(), KEY_PREFIX + ip.key(), KEY_PREFIX + user.key()};
        byte[][] args = {
                bytes(global.capacity()), bytes(global.window().toMillis()),
                bytes(ip.capacity()), bytes(ip.window().toMillis()),
                bytes(user.capacity()), bytes(user.window().toMillis())
        };

        RedisCommands<String, byte[]> redis = connection.sync();
        Long result;
        try {
            result = redis.evalsha(sha, ScriptOutputType.INTEGER, keys, args);
        } catch (RedisNoScriptException e) {
            // first call, or Redis restarted / failed over and lost its script cache
            redis.scriptLoad(script);
            result = redis.evalsha(sha, ScriptOutputType.INTEGER, keys, args);
        }

        return result == null || result == 0 ? null : NAMES[result.intValue() - 1];
    }

    private static byte[] bytes(long value) {
        return Long.toString(value).getBytes(StandardCharsets.UTF_8);
    }
}
