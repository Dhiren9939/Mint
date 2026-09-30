package me.dhiren9939.mint.repository;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.dhiren9939.mint.common.RedisConnectionProvider;
import me.dhiren9939.mint.entity.FileMetaData;
import me.dhiren9939.mint.entity.FileMetaDataBuilder;
import me.dhiren9939.mint.entity.FileState;
import me.dhiren9939.mint.service.ExpiryDuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisFileMetaDataCacheTest {

    private static final String FILE_CODE = "abc123";
    private static final String EXPECTED_KEY = "file-meta:" + FILE_CODE;

    @Mock
    private RedisConnectionProvider redisConnectionProvider;

    @Mock
    private StatefulRedisConnection<String, byte[]> statefulConnection;

    @Mock
    private RedisCommands<String, byte[]> commands;

    private ObjectMapper objectMapper;

    private SimpleMeterRegistry meterRegistry;

    private RedisFileMetaDataCache cache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();
        cache = new RedisFileMetaDataCache(redisConnectionProvider, objectMapper, meterRegistry);
        ReflectionTestUtils.setField(cache, "ttlSeconds", 300L);
    }

    private FileMetaData fileMetaData(LocalDateTime cleanAt) {
        return FileMetaDataBuilder.builder()
                .fileCode(FILE_CODE)
                .fileKey("uploads/key.txt")
                .cleanAt(cleanAt)
                .fileState(FileState.READY)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();
    }

    @Test
    @DisplayName("putIfAbsent: sets the value under the file-meta key when the entry is not yet expired")
    void putIfAbsent_setsValueWhenNotExpired() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);

        FileMetaData fileMetaData = fileMetaData(LocalDateTime.now().plusMinutes(10));

        cache.putIfAbsent(fileMetaData);

        verify(commands, times(1)).set(eq(EXPECTED_KEY), any(byte[].class), any(SetArgs.class));
        verify(commands, never()).del(anyString());
    }

    @Test
    @DisplayName("putIfAbsent: does nothing when the entry's cleanAt is already in the past")
    void putIfAbsent_doesNothingWhenAlreadyExpired() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);

        FileMetaData fileMetaData = fileMetaData(LocalDateTime.now().minusMinutes(1));

        cache.putIfAbsent(fileMetaData);

        verify(commands, never()).set(anyString(), any(byte[].class), any(SetArgs.class));
        verify(commands, never()).del(anyString());
    }

    @Test
    @DisplayName("putIfAbsent: fails open and does not evict when the Redis write throws")
    void putIfAbsent_failsOpenWhenSetThrows() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);
        when(commands.set(anyString(), any(byte[].class), any(SetArgs.class)))
                .thenThrow(new RuntimeException("boom"));

        FileMetaData fileMetaData = fileMetaData(LocalDateTime.now().plusMinutes(10));

        cache.putIfAbsent(fileMetaData);

        verify(commands, times(1)).set(eq(EXPECTED_KEY), any(byte[].class), any(SetArgs.class));
        verify(commands, never()).del(anyString());
    }

    @Test
    @DisplayName("putIfAbsent: does nothing and does not throw when no Redis connection is available")
    void putIfAbsent_noConnectionAvailable() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.empty());

        FileMetaData fileMetaData = fileMetaData(LocalDateTime.now().plusMinutes(10));

        cache.putIfAbsent(fileMetaData);

        verifyNoInteractions(commands);
    }

    // --- metrics: mint.cache.get / mint.cache.put --------------------------------------

    @Test
    @DisplayName("get: records mint.cache.get with result=hit on a cache hit")
    void get_recordsHitMetric() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);
        FileMetaData fileMetaData = fileMetaData(LocalDateTime.now().plusMinutes(10));
        when(commands.get(EXPECTED_KEY)).thenReturn(objectMapper.writeValueAsBytes(fileMetaData));

        Optional<FileMetaData> result = cache.get(FILE_CODE);

        assertTrueHit(result);
        assertEquals(1, meterRegistry.get("mint.cache.get").tag("result", "hit").timer().count());
    }

    @Test
    @DisplayName("get: records mint.cache.get with result=miss when the key is absent")
    void get_recordsMissMetric() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);
        when(commands.get(EXPECTED_KEY)).thenReturn(null);

        Optional<FileMetaData> result = cache.get(FILE_CODE);

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        assertEquals(1, meterRegistry.get("mint.cache.get").tag("result", "miss").timer().count());
    }

    @Test
    @DisplayName("get: records mint.cache.get with result=disconnected when there is no connection")
    void get_recordsDisconnectedMetric() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.empty());

        Optional<FileMetaData> result = cache.get(FILE_CODE);

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        assertEquals(1, meterRegistry.get("mint.cache.get").tag("result", "disconnected").timer().count());
    }

    @Test
    @DisplayName("get: records mint.cache.get with result=error when reading throws")
    void get_recordsErrorMetric() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);
        when(commands.get(EXPECTED_KEY)).thenThrow(new RuntimeException("boom"));

        Optional<FileMetaData> result = cache.get(FILE_CODE);

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        assertEquals(1, meterRegistry.get("mint.cache.get").tag("result", "error").timer().count());
    }

    @Test
    @DisplayName("put: records mint.cache.put with op=put, result=ok on a successful write")
    void put_recordsOkMetric() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);
        FileMetaData fileMetaData = fileMetaData(LocalDateTime.now().plusMinutes(10));

        cache.put(fileMetaData);

        assertEquals(1, meterRegistry.get("mint.cache.put")
                .tag("op", "put").tag("result", "ok").timer().count());
    }

    @Test
    @DisplayName("put: records mint.cache.put with op=put, result=error when the write throws")
    void put_recordsErrorMetric() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);
        when(commands.set(anyString(), any(byte[].class), any(SetArgs.class)))
                .thenThrow(new RuntimeException("boom"));
        FileMetaData fileMetaData = fileMetaData(LocalDateTime.now().plusMinutes(10));

        cache.put(fileMetaData);

        assertEquals(1, meterRegistry.get("mint.cache.put")
                .tag("op", "put").tag("result", "error").timer().count());
    }

    @Test
    @DisplayName("putIfAbsent: records mint.cache.put with op=put_if_absent, result=ok")
    void putIfAbsent_recordsOkMetric() {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(statefulConnection));
        when(statefulConnection.sync()).thenReturn(commands);
        FileMetaData fileMetaData = fileMetaData(LocalDateTime.now().plusMinutes(10));

        cache.putIfAbsent(fileMetaData);

        assertEquals(1, meterRegistry.get("mint.cache.put")
                .tag("op", "put_if_absent").tag("result", "ok").timer().count());
    }

    private void assertTrueHit(Optional<FileMetaData> result) {
        org.junit.jupiter.api.Assertions.assertTrue(result.isPresent());
    }
}
