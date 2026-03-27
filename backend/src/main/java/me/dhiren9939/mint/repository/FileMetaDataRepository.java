package me.dhiren9939.mint.repository;

import jakarta.persistence.LockModeType;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.model.entity.metadata.FileState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FileMetaDataRepository extends JpaRepository<FileMetaData, Long> {
    Optional<FileMetaData> findByFileCode(String fileCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FileMetaData f WHERE f.fileCode = :fileCode")
    Optional<FileMetaData> findByFileCodeWithLock(@Param("fileCode") String fileCode);

    Optional<FileMetaData> findByFileKeyAndFileCode(String fileKey, String fileCode);

    @Query("SELECT f from FileMetaData f WHERE f.cleanAt < :now OR f.fileState = :fileState")
    Slice<FileMetaData> findExpiredOrInState(
            @Param("now") LocalDateTime now,
            @Param("fileState") FileState fileState,
            Pageable pageable);

}
