package me.dhiren9939.mint.service.s3;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.dto.response.ConfirmUploadResponse;
import me.dhiren9939.mint.dto.response.GenerateDownloadLinkResponse;
import me.dhiren9939.mint.dto.response.GenerateUploadLinkResponse;
import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.model.entity.FileMetaDataBuilder;
import me.dhiren9939.mint.model.entity.FileState;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import me.dhiren9939.mint.service.CodeGeneratorService;
import me.dhiren9939.mint.service.ExpiryDuration;
import me.dhiren9939.mint.service.FileSharingService;
import me.dhiren9939.mint.service.FileStorageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class S3SharingService implements FileSharingService {
    private final FileStorageService fileStorageService;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final CodeGeneratorService codeGeneratorService;

    private LocalDateTime getExpiresAt(ExpiryDuration duration) {
        final LocalDateTime now = LocalDateTime.now();
        switch (duration) {
            case MINUTES15 -> {
                return now.plusMinutes(15);
            }
            case MINUTES30 -> {
                return now.plusMinutes(30);
            }
            case MINUTES60 -> {
                return now.plusMinutes(60);
            }
            case HOURS24 -> {
                return now.plusHours(24);
            }
            default -> {
                return now;
            }
        }
    }

    @Override
    public GenerateUploadLinkResponse generateUploadLink(ExpiryDuration duration, int maxDownLoad, String fileName, String contentType, int contentSize) {
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String key = "uploads/" + UUID.randomUUID() + extension;

        String fileUrl = fileStorageService.generateUploadLink(key, contentType, contentSize);
        String fileCode = codeGeneratorService.getRandomCode();

        FileMetaData fileMetaData = FileMetaDataBuilder.builder()
                .fileCode(fileCode)
                .cleanAt(LocalDateTime.now().plusMinutes(1))
                .maxDownloadCount(maxDownLoad)
                .fileState(FileState.PENDING)
                .fileExpiryDuration(duration)
                .build();

        fileMetaData = fileMetaDataRepository.save(fileMetaData);

        return GenerateUploadLinkResponse.of(fileUrl, fileMetaData);
    }

    @Override
    public ConfirmUploadResponse confirmUpload(String fileKey, String fileCode) throws FileMetaDataNotFoundException {
        Optional<FileMetaData> optionalFileMetaData = fileMetaDataRepository.findByFileKeyAndFileCode(fileKey, fileCode);
        if (optionalFileMetaData.isEmpty())
            throw new FileMetaDataNotFoundException("Invalid metadata information. File not Found.");

        FileMetaData metaData = optionalFileMetaData.get();
        metaData.setFileState(FileState.READY);
        fileMetaDataRepository.save(metaData);
        return ConfirmUploadResponse.of(metaData);
    }

    @Override
    public GenerateDownloadLinkResponse generateDownloadLink(String fileCode) throws FileMetaDataNotFoundException {
        Optional<FileMetaData> optionalMetaData = fileMetaDataRepository.findByFileCode(fileCode);
        if (optionalMetaData.isEmpty())
            throw new FileMetaDataNotFoundException();

        FileMetaData fileMetaData = optionalMetaData.get();

        // Check states
        FileState fileState = fileMetaData.getFileState();
        if (fileState == FileState.PENDING)
            throw new FileMetaDataNotFoundException("File not uploaded. File url cannot be generated.");
        if (fileState == FileState.DELETED)
            throw new FileMetaDataNotFoundException();

        // Check downloadCount and expiresAt
        int fileDownLoadCount = fileMetaData.getDownloadCount();
        LocalDateTime fileExpiresAt = fileMetaData.getCleanAt();
        if (LocalDateTime.now().isAfter(fileExpiresAt) || fileDownLoadCount >= fileMetaData.getMaxDownloadCount()) {
            // Mark Deleted.
            fileMetaData.setFileState(FileState.DELETED);
            fileMetaDataRepository.save(fileMetaData);

            throw new FileMetaDataNotFoundException();
        }

        String fileUrl = fileStorageService.generateDownloadLink(fileMetaData.getFileKey());

        fileMetaData.setDownloadCount(fileDownLoadCount + 1);
        fileMetaDataRepository.save(fileMetaData);

        return GenerateDownloadLinkResponse.of(fileUrl, fileMetaData);
    }
}
