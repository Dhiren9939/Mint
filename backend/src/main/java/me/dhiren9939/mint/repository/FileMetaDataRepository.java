package me.dhiren9939.mint.repository;

import jakarta.persistence.LockModeType;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileMetaDataRepository extends JpaRepository<FileMetaData, Long> {
    Optional<FileMetaData> findByFileCode(String fileCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FileMetaData f WHERE f.fileCode = :fileCode")
    Optional<FileMetaData> findByFileCodeWithLock(@Param("fileCode") String fileCode);

    Optional<FileMetaData> findByFileKeyAndFileCode(String fileKey, String fileCode);
}
