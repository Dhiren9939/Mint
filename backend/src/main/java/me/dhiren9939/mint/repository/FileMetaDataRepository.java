package me.dhiren9939.mint.repository;

import me.dhiren9939.mint.model.entity.FileMetaData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileMetaDataRepository extends JpaRepository<FileMetaData, Long> {
    Optional<FileMetaData> findByFileCode(String fileCode);

    Optional<FileMetaData> findByFileKeyAndFileCode(String fileKey, String fileCode);
}
