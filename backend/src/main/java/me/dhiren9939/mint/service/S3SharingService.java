package me.dhiren9939.mint.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.model.entity.FileState;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

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
    public FileMetaData generateUploadLink(ExpiryDuration duration, int maxDownLoad) {
        String fileUrl = fileStorageService.generatePreSignedURL();
        String fileCode = codeGeneratorService.getRandomCode();
        FileMetaData fileMetaData =
                new FileMetaData(0L, fileCode, fileUrl, this.getExpiresAt(duration), 0, maxDownLoad, FileState.PENDING);
        return fileMetaDataRepository.save(fileMetaData);
    }

    @Override
    public FileMetaData confirmUpload(long fileMetaDataId, String fileCode, String fileUrl) throws FileMetaDataNotFoundException {
        FileMetaData exampleData = new FileMetaData();
        exampleData.setFileCode(fileCode);
        exampleData.setFileUrl(fileUrl);
        exampleData.setId(fileMetaDataId);

        ExampleMatcher matcher = ExampleMatcher
                .matching()
                .withIgnorePaths("isOneTimeDownload", "expiresAt", "downloadCount");
        Example<FileMetaData> example = Example.of(exampleData, matcher);
        Optional<FileMetaData> optionalFileMetaData = fileMetaDataRepository.findOne(example);
        if (optionalFileMetaData.isEmpty())
            throw new FileMetaDataNotFoundException("Invalid metadata information. File not Found.");

        FileMetaData metaData = optionalFileMetaData.get();
        metaData.setFileState(FileState.READY);
        fileMetaDataRepository.save(metaData);
        return metaData;
    }

    @Override
    public FileMetaData getDownloadLink(String fileCode) throws FileMetaDataNotFoundException {
        Optional<FileMetaData> optionalMetaData = fileMetaDataRepository.findByFileCode(fileCode);
        if (optionalMetaData.isEmpty())
            throw new FileMetaDataNotFoundException();

        FileMetaData fileMetaData = optionalMetaData.get();

        // Check states
        FileState fileState = fileMetaData.getFileState();
        if (fileState == FileState.PENDING)
            throw new FileMetaDataNotFoundException("File not uploaded. File cannot be send.");
        if (fileState == FileState.DELETED)
            throw new FileMetaDataNotFoundException();

        // Check downloadCount and expiresAt
        int fileDownLoadCount = fileMetaData.getDownloadCount();
        LocalDateTime fileExpiresAt = fileMetaData.getExpiresAt();
        if (LocalDateTime.now().isAfter(fileExpiresAt) || fileDownLoadCount >= fileMetaData.getMaxDownloadCount()) {
            // Mark Deleted.
            fileMetaData.setFileState(FileState.DELETED);
            fileMetaDataRepository.save(fileMetaData);

            throw new FileMetaDataNotFoundException();
        }

        fileMetaData.setDownloadCount(fileDownLoadCount + 1);
        return fileMetaData;
    }
}
