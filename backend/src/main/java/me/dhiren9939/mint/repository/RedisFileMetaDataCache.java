package me.dhiren9939.mint.repository;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
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

    @Value("${mint.cache.ttl-seconds:300}")
    private long ttlSeconds;

    @Override
    public Optional<FileMetaData> get(String fileCode) {
        try {
            return commands()
                    .map(commands -> commands.get(key(fileCode)))
                    .map(bytes -> objectMapper.readValue(bytes, FileMetaData.class));
        } catch (RuntimeException e) {
            log.warn("Cache read failed for {}, falling back: {}", fileCode, e.toString());
            return Optional.empty();
        }
    }

    @Override
    public void put(FileMetaData fileMetaData) {
        commands().ifPresent(commands -> {
            String key = key(fileMetaData.getFileCode());
            long cleanAtEpoch = epochSecond(fileMetaData.getCleanAt());
            long now = System.currentTimeMillis() / 1000;

            if (cleanAtEpoch <= now) {
                tryEvict(commands, key);
                return;
            }

            long expiresAt = Math.min(now + ttlSeconds, cleanAtEpoch);

            try {
                byte[] value = objectMapper.writeValueAsBytes(fileMetaData);
                commands.set(key, value, SetArgs.Builder.exAt(expiresAt));
            } catch (RuntimeException e) {
                log.warn("Cache write failed for {}, evicting instead: {}", key, e.toString());
                tryEvict(commands, key);
            }
        });
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
