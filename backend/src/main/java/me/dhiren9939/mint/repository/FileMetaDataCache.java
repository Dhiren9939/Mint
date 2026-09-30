package me.dhiren9939.mint.repository;

import me.dhiren9939.mint.entity.FileMetaData;

import java.util.Optional;

public interface FileMetaDataCache {

    Optional<FileMetaData> get(String fileCode);

    void put(FileMetaData fileMetaData);

    /**
     * Caches the entry only if none exists yet. Used to fill the cache after a read miss, so a
     * value read from the database just before a concurrent {@link #put} can't overwrite it.
     */
    void putIfAbsent(FileMetaData fileMetaData);

    void evict(String fileCode);
}
