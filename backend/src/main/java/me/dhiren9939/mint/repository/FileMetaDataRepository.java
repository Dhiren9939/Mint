package me.dhiren9939.mint.repository;

import me.dhiren9939.mint.entity.FileMetaData;

import java.util.Optional;

public interface FileMetaDataRepository {
    FileMetaData save(FileMetaData fileMetaData);

    Optional<FileMetaData> findByFileCode(String fileCode);

    Optional<FileMetaData> findByFileKeyAndFileCode(String fileKey, String fileCode);

    /**
     * A code is free when nothing exists under it, or the file under it is already DELETED.
     */
    boolean isFileCodeFree(String fileCode);
}
