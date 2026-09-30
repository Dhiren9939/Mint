package me.dhiren9939.mint.repository;

import me.dhiren9939.mint.entity.FileMetaData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Used when {@code mint.cache.enabled=false}, so the store behaves as plain Dynamo access
 * (useful for benchmarking the cache's effect).
 */
@Component
@ConditionalOnProperty(name = "mint.cache.enabled", havingValue = "false")
public class NoOpFileMetaDataCache implements FileMetaDataCache {

    @Override
    public Optional<FileMetaData> get(String fileCode) {
        return Optional.empty();
    }

    @Override
    public void put(FileMetaData fileMetaData) {
        // no-op
    }

    @Override
    public void putIfAbsent(FileMetaData fileMetaData) {
        // no-op
    }

    @Override
    public void evict(String fileCode) {
        // no-op
    }
}
