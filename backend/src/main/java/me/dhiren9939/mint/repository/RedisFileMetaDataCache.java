package me.dhiren9939.mint.repository;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.common.RedisConnectionProvider;
import me.dhiren9939.mint.entity.FileMetaData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Manages FileMetaData over the connection {@link RedisConnectionProvider} already holds.
 * Every call fails open: on a missing connection or any Redis/serialization error it logs
 * and behaves as a miss/no-op, so a cache outage never breaks a request.
 * <p>
 * Entries live for a short, fixed TTL rather than the file's real expiry, so a stale entry
 * self-heals quickly even if an eviction after a failed write also fails. {@code cleanAt} only
 * caps that TTL, it never extends it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mint.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisFileMetaDataCache implements FileMetaDataCache {

    private static final String KEY_PREFIX = "file-meta:";

    private final RedisConnectionProvider redisConnectionProvider;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${mint.cache.ttl-seconds:300}")
    private long ttlSeconds;

    @Override
    public Optional<FileMetaData> get(String fileCode) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "error";
        try {
            Optional<RedisCommands<String, byte[]>> commands = commands();
            if (commands.isEmpty()) {
                result = "disconnected";
                return Optional.empty();
            }

            byte[] bytes = commands.get().get(key(fileCode));
            if (bytes == null) {
                result = "miss";
                return Optional.empty();
            }

            FileMetaData value = objectMapper.readValue(bytes, FileMetaData.class);
            result = "hit";
            return Optional.of(value);
        } catch (RuntimeException e) {
            log.warn("Cache read failed for {}, falling back: {}", fileCode, e.toString());
            result = "error";
            return Optional.empty();
        } finally {
            sample.stop(cacheGetTimer(result));
        }
    }

    @Override
    public void put(FileMetaData fileMetaData) {
        timePut(fileMetaData, "put",
                (commands, key, expiresAt, value) -> commands.set(key, value, SetArgs.Builder.exAt(expiresAt)),
                this::tryEvict,
                (commands, key, e) -> {
                    log.warn("Cache write failed for {}, evicting instead: {}", key, e.toString());
                    tryEvict(commands, key);
                });
    }

    @Override
    public void putIfAbsent(FileMetaData fileMetaData) {
        // A failed fill leaves nothing behind, and evicting here could drop a newer entry
        // a concurrent put just wrote, so both the expired case and the write failure are
        // only logged, never evicted
        timePut(fileMetaData, "put_if_absent",
                (commands, key, expiresAt, value) -> commands.set(key, value, SetArgs.Builder.exAt(expiresAt).nx()),
                (commands, key) -> { },
                (commands, key, e) -> log.warn("Cache fill failed for {}: {}", key, e.toString()));
    }

    @FunctionalInterface
    private interface SetCall {
        void set(RedisCommands<String, byte[]> commands, String key, long expiresAt, byte[] value);
    }

    @FunctionalInterface
    private interface EvictCall {
        void evict(RedisCommands<String, byte[]> commands, String key);
    }

    @FunctionalInterface
    private interface SetErrorCall {
        void onError(RedisCommands<String, byte[]> commands, String key, RuntimeException e);
    }

    private void timePut(FileMetaData fileMetaData, String op, SetCall setCall, EvictCall onExpired,
                         SetErrorCall onSetError) {
        commands().ifPresent(commands -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            String result = "ok";
            try {
                String key = key(fileMetaData.getFileCode());
                long cleanAtEpoch = epochSecond(fileMetaData.getCleanAt());
                long now = System.currentTimeMillis() / 1000;

                if (cleanAtEpoch <= now) {
                    onExpired.evict(commands, key);
                    return;
                }

                long expiresAt = Math.min(now + ttlSeconds, cleanAtEpoch);

                try {
                    byte[] value = objectMapper.writeValueAsBytes(fileMetaData);
                    setCall.set(commands, key, expiresAt, value);
                } catch (RuntimeException e) {
                    result = "error";
                    onSetError.onError(commands, key, e);
                }
            } finally {
                sample.stop(Timer.builder("mint.cache.put")
                        .tag("op", op)
                        .tag("result", result)
                        .register(meterRegistry));
            }
        });
    }

    private Timer cacheGetTimer(String result) {
        return Timer.builder("mint.cache.get")
                .tag("result", result)
                .register(meterRegistry);
    }

    @Override
    public void evict(String fileCode) {
        commands().ifPresent(commands -> tryEvict(commands, key(fileCode)));
    }

    private void tryEvict(RedisCommands<String, byte[]> commands, String key) {
        try {
            commands.del(key);
        } catch (RuntimeException e) {
            log.warn("Cache evict failed for {}: {}", key, e.toString());
        }
    }

    private Optional<RedisCommands<String, byte[]>> commands() {
        return redisConnectionProvider.getConnection().map(StatefulRedisConnection::sync);
    }

    private String key(String fileCode) {
        return KEY_PREFIX + fileCode;
    }

    private long epochSecond(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
