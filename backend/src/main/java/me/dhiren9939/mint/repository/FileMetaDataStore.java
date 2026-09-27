package me.dhiren9939.mint.repository;

import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.entity.FileMetaData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Primary
@Repository
public class FileMetaDataStore implements FileMetaDataRepository {

    private final FileMetaDataRepository dbRepo;
    private final FileMetaDataCache cache;

    public FileMetaDataStore(@Qualifier("dynamoDbRepository") FileMetaDataRepository dbRepo, FileMetaDataCache cache) {
        this.dbRepo = dbRepo;
        this.cache = cache;
    }

    @Override
    public FileMetaData save(FileMetaData fileMetaData) {
        FileMetaData saved = dbRepo.save(fileMetaData);
        cache.put(saved);
        return saved;
    }

    @Override
    public Optional<FileMetaData> findByFileCode(String fileCode) {
        Optional<FileMetaData> cached = cache.get(fileCode);
        return cached.isPresent() ? cached : dbRepo.findByFileCode(fileCode);
    }

    @Override
    public Optional<FileMetaData> findByFileKeyAndFileCode(String fileKey, String fileCode) {
        Optional<FileMetaData> cached = cache.get(fileCode);
        if (cached.isPresent()) {
            return cached.filter(fileMetaData -> fileMetaData.getFileKey().equals(fileKey));
        }
        return dbRepo.findByFileKeyAndFileCode(fileKey, fileCode);
    }

    @Override
    public boolean isFileCodeFree(String fileCode) {
        // Bypasses the cache: a fresh code is almost always a cache miss anyway, and
        // uniqueness checks need the Dynamo consistent read, not a possibly-stale cache entry.
        return dbRepo.isFileCodeFree(fileCode);
    }
}
