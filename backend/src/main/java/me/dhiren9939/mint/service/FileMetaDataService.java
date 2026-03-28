package me.dhiren9939.mint.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.model.entity.metadata.FileMetaDataBuilder;
import me.dhiren9939.mint.model.entity.metadata.FileState;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(noRollbackFor = FileMetaDataNotFoundException.class)
public class FileMetaDataService {
    private final FileMetaDataRepository fileMetaDataRepository;

    public FileMetaData createPending(String fileKey, String fileCode, ExpiryDuration duration, int maxDownload) {
        FileMetaData fileMetaData = FileMetaDataBuilder.builder()
                .fileCode(fileCode)
                .cleanAt(LocalDateTime.now().plusMinutes(1))
                .maxDownloadCount(maxDownload)
                .fileState(FileState.PENDING)
                .fileExpiryDuration(duration)
                .fileKey(fileKey)
                .build();

        return fileMetaDataRepository.save(fileMetaData);
    }

    public FileMetaData markReady(String fileKey, String fileCode) throws FileMetaDataNotFoundException {
        Optional<FileMetaData> optionalFileMetaData = fileMetaDataRepository.findByFileKeyAndFileCode(fileKey, fileCode);
        if (optionalFileMetaData.isEmpty())
            throw new FileMetaDataNotFoundException("Invalid metadata information. File not Found.");

        FileMetaData metaData = optionalFileMetaData.get();
        if (metaData.getCleanAt().isBefore(LocalDateTime.now())) {
            metaData.setFileState(FileState.DELETED);
            fileMetaDataRepository.save(metaData);
            throw new FileMetaDataNotFoundException();
        }

        LocalDateTime expiresAt = this.getExpiresAt(metaData.getFileExpiryDuration());
        metaData.setFileState(FileState.READY);
        metaData.setCleanAt(expiresAt);

        return fileMetaDataRepository.save(metaData);
    }

    public FileMetaData getForDownload(String fileCode) throws FileMetaDataNotFoundException {
        Optional<FileMetaData> optionalMetaData = fileMetaDataRepository.findByFileCodeWithLock(fileCode);
        if (optionalMetaData.isEmpty())
            throw new FileMetaDataNotFoundException();

        FileMetaData fileMetaData = optionalMetaData.get();

        // Check states
        FileState fileState = fileMetaData.getFileState();
        if (fileState == FileState.PENDING)
            throw new FileMetaDataNotFoundException("File not uploaded. File url cannot be generated.");
        if (fileState == FileState.DELETED)
            throw new FileMetaDataNotFoundException();

        // Check downloadCount and cleanAt
        int fileDownLoadCount = fileMetaData.getDownloadCount();
        LocalDateTime fileExpiresAt = fileMetaData.getCleanAt();
        if (LocalDateTime.now().isAfter(fileExpiresAt) || fileDownLoadCount >= fileMetaData.getMaxDownloadCount()) {
            // Mark Deleted.
            fileMetaData.setFileState(FileState.DELETED);
            fileMetaDataRepository.save(fileMetaData);

            throw new FileMetaDataNotFoundException();
        }

        fileMetaData.setDownloadCount(fileDownLoadCount + 1);
        return fileMetaDataRepository.save(fileMetaData);
    }

    private LocalDateTime getExpiresAt(ExpiryDuration duration) {
        return switch (duration) {
            case MINUTES15 -> LocalDateTime.now().plusMinutes(15);
            case MINUTES30 -> LocalDateTime.now().plusMinutes(30);
            case MINUTES60 -> LocalDateTime.now().plusMinutes(60);
            case HOURS24 -> LocalDateTime.now().plusHours(24);
        };
    }
}
