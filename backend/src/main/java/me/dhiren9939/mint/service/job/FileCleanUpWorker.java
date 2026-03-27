package me.dhiren9939.mint.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.model.entity.metadata.FileState;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import me.dhiren9939.mint.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanUpWorker {
    private final FileStorageService fileStorageService;
    private final FileMetaDataRepository fileMetaDataRepository;

    @Value("${mint.cleanup.size:50}")
    private Integer batchSize;

    @Transactional
    public boolean processBatchCleanUp() {
        LocalDateTime expiredBy = LocalDateTime.now().minusMinutes(2);
        Slice<FileMetaData> slice = fileMetaDataRepository.findExpiredOrInState(
                expiredBy,
                FileState.DELETED,
                PageRequest.of(0, batchSize)
        );

        for (FileMetaData fileMetaData : slice) {
            try {
                // Delete record first
                fileMetaDataRepository.delete(fileMetaData);
                // In case of a S3 error let life cycle handle delete
                if (fileMetaData.getFileState() != FileState.PENDING)
                    fileStorageService.deleteFile(fileMetaData.getFileKey());
            } catch (Exception ex) {
                log.error("Failed to hard delete file {}: {}", fileMetaData.getFileKey(), ex.getMessage());
            }
        }

        return !slice.isEmpty();
    }
}
