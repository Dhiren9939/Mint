package me.dhiren9939.mint.repository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileMetaDataStoreTest {

    @Mock
    private FileMetaDataRepository dbRepo;

    @Mock
    private FileMetaDataCache cache;

    private SimpleMeterRegistry meterRegistry;

    private FileMetaDataStore fileMetaDataStore;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        fileMetaDataStore = new FileMetaDataStore(dbRepo, cache, meterRegistry);
    }

    private FileMetaData entry(String fileCode, String fileKey) {
        return FileMetaDataBuilder.builder()
                .fileCode(fileCode)
                .fileKey(fileKey)
                .cleanAt(LocalDateTime.now().plusMinutes(10))
                .fileState(FileState.READY)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();
    }

    @Test
    @DisplayName("findByFileCode: cache hit returns cached value without touching db or filling cache")
    void findByFileCode_cacheHit_returnsCachedValue() {
        FileMetaData cached = entry("abc123", "key.txt");
        when(cache.get("abc123")).thenReturn(Optional.of(cached));

        Optional<FileMetaData> result = fileMetaDataStore.findByFileCode("abc123");

        assertTrue(result.isPresent());
        assertEquals(cached, result.get());
        verify(dbRepo, never()).findByFileCode(any());
        verify(cache, never()).putIfAbsent(any());
    }

    @Test
    @DisplayName("findByFileCode: cache miss with db hit returns db value and fills cache via putIfAbsent")
    void findByFileCode_cacheMissDbHit_fillsCacheWithPutIfAbsent() {
        FileMetaData fromDb = entry("abc123", "key.txt");
        when(cache.get("abc123")).thenReturn(Optional.empty());
        when(dbRepo.findByFileCode("abc123")).thenReturn(Optional.of(fromDb));

        Optional<FileMetaData> result = fileMetaDataStore.findByFileCode("abc123");

        assertTrue(result.isPresent());
        assertEquals(fromDb, result.get());
        verify(cache, times(1)).putIfAbsent(fromDb);
    }

    @Test
    @DisplayName("findByFileCode: cache miss and db miss returns empty without filling cache")
    void findByFileCode_cacheMissDbMiss_returnsEmpty() {
        when(cache.get("abc123")).thenReturn(Optional.empty());
        when(dbRepo.findByFileCode("abc123")).thenReturn(Optional.empty());

        Optional<FileMetaData> result = fileMetaDataStore.findByFileCode("abc123");

        assertTrue(result.isEmpty());
        verify(cache, never()).putIfAbsent(any());
    }

    @Test
    @DisplayName("findByFileKeyAndFileCode: cache hit with matching fileKey returns it without touching db or cache fill")
    void findByFileKeyAndFileCode_cacheHitMatchingKey_returnsCachedValue() {
        FileMetaData cached = entry("abc123", "key.txt");
        when(cache.get("abc123")).thenReturn(Optional.of(cached));

        Optional<FileMetaData> result = fileMetaDataStore.findByFileKeyAndFileCode("key.txt", "abc123");

        assertTrue(result.isPresent());
        assertEquals(cached, result.get());
        verify(dbRepo, never()).findByFileKeyAndFileCode(any(), any());
        verify(cache, never()).putIfAbsent(any());
    }

    @Test
    @DisplayName("findByFileKeyAndFileCode: cache hit with different fileKey returns empty without touching db or cache fill")
    void findByFileKeyAndFileCode_cacheHitDifferentKey_returnsEmpty() {
        FileMetaData cached = entry("abc123", "other-key.txt");
        when(cache.get("abc123")).thenReturn(Optional.of(cached));

        Optional<FileMetaData> result = fileMetaDataStore.findByFileKeyAndFileCode("key.txt", "abc123");

        assertTrue(result.isEmpty());
        verify(dbRepo, never()).findByFileKeyAndFileCode(any(), any());
        verify(cache, never()).putIfAbsent(any());
    }

    @Test
    @DisplayName("findByFileKeyAndFileCode: cache miss with db hit returns db value and fills cache via putIfAbsent")
    void findByFileKeyAndFileCode_cacheMissDbHit_fillsCacheWithPutIfAbsent() {
        FileMetaData fromDb = entry("abc123", "key.txt");
        when(cache.get("abc123")).thenReturn(Optional.empty());
        when(dbRepo.findByFileKeyAndFileCode("key.txt", "abc123")).thenReturn(Optional.of(fromDb));

        Optional<FileMetaData> result = fileMetaDataStore.findByFileKeyAndFileCode("key.txt", "abc123");

        assertTrue(result.isPresent());
        assertEquals(fromDb, result.get());
        verify(cache, times(1)).putIfAbsent(fromDb);
    }

    // Note: no test for "db returns an entry with a non-matching fileKey" on
    // findByFileKeyAndFileCode. Unlike findByFileCode (keyed only by fileCode, so the cache can
    // hold a stale/different fileKey for the same code), dbRepo.findByFileKeyAndFileCode is
    // itself keyed by both fileKey AND fileCode (see FileMetaDataRepository /
    // DynamoFileMetaDataRepository), so the db can never return a record whose fileKey doesn't
    // match the requested fileKey. Fabricating that scenario wouldn't exercise real behavior.

    @Test
    @DisplayName("save: still persists via dbRepo.save then unconditionally overwrites cache via put, not putIfAbsent")
    void save_persistsAndUnconditionallyPutsCache() {
        FileMetaData toSave = entry("abc123", "key.txt");
        when(dbRepo.save(toSave)).thenReturn(toSave);

        FileMetaData result = fileMetaDataStore.save(toSave);

        assertEquals(toSave, result);
        verify(dbRepo, times(1)).save(toSave);
        verify(cache, times(1)).put(toSave);
        verify(cache, never()).putIfAbsent(any());
    }
}
