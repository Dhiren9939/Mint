package me.dhiren9939.mint.repository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.entity.FileMetaData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Primary
@Repository
public class FileMetaDataStore implements FileMetaDataRepository {

    private final FileMetaDataRepository dbRepo;
    private final FileMetaDataCache cache;
    private final MeterRegistry meterRegistry;

    public FileMetaDataStore(@Qualifier("dynamoDbRepository") FileMetaDataRepository dbRepo,
                             FileMetaDataCache cache,
                             MeterRegistry meterRegistry) {
        this.dbRepo = dbRepo;
        this.cache = cache;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public FileMetaData save(FileMetaData fileMetaData) {
        FileMetaData saved = timed("save", () -> dbRepo.save(fileMetaData));
        cache.put(saved);
        return saved;
    }

    @Override
    public Optional<FileMetaData> findByFileCode(String fileCode) {
        Optional<FileMetaData> cached = cache.get(fileCode);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<FileMetaData> found = timed("find", () -> dbRepo.findByFileCode(fileCode));
        found.ifPresent(cache::putIfAbsent);
        return found;
    }

    @Override
    public Optional<FileMetaData> findByFileKeyAndFileCode(String fileKey, String fileCode) {
        Optional<FileMetaData> cached = cache.get(fileCode);
        if (cached.isPresent()) {
            return cached.filter(fileMetaData -> fileMetaData.getFileKey().equals(fileKey));
        }
        Optional<FileMetaData> found = timed("find", () -> dbRepo.findByFileKeyAndFileCode(fileKey, fileCode));
        found.ifPresent(cache::putIfAbsent);
        return found;
    }

    @Override
    public boolean isFileCodeFree(String fileCode) {
        // Bypasses the cache: a fresh code is almost always a cache miss anyway, and
        // uniqueness checks need the Dynamo consistent read, not a possibly-stale cache entry.
        return timed("isFree", () -> dbRepo.isFileCodeFree(fileCode));
    }

    private <T> T timed(String op, Supplier<T> call) {
        return Timer.builder("mint.db.duration")
                .tag("op", op)
                .register(meterRegistry)
                .record(call);
    }
}
