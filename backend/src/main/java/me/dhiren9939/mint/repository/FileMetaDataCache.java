package me.dhiren9939.mint.repository;

import me.dhiren9939.mint.entity.FileMetaData;

import java.util.Optional;

public interface FileMetaDataCache {

    Optional<FileMetaData> get(String fileCode);

    void put(FileMetaData fileMetaData);

    void evict(String fileCode);
}
