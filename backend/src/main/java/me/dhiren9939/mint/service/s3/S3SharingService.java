package me.dhiren9939.mint.service.s3;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.dto.response.ConfirmUploadResponse;
import me.dhiren9939.mint.dto.response.GenerateDownloadLinkResponse;
import me.dhiren9939.mint.dto.response.GenerateUploadLinkResponse;
import me.dhiren9939.mint.exception.FileCodeGenerationFailure;
import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(noRollbackFor = FileMetaDataNotFoundException.class)
public class S3SharingService implements FileSharingService {
    private final FileStorageService fileStorageService;
    private final CodeGeneratorService codeGeneratorService;
    private final FileMetaDataService fileMetaDataService;


    @Override
    public GenerateUploadLinkResponse generateUploadLink(
            ExpiryDuration duration,
            int maxDownLoad,
            String fileName,
            String contentType,
            int contentSize) throws FileCodeGenerationFailure {

        String extension = fileName.substring(fileName.lastIndexOf("."));
        String key = "uploads/" + UUID.randomUUID() + extension;

        String fileUrl = fileStorageService.generateUploadLink(key, contentType, contentSize);
        String fileCode = codeGeneratorService.getRandomCode();

        FileMetaData fileMetaData = fileMetaDataService.createPending(key, fileCode, duration, maxDownLoad);
        return GenerateUploadLinkResponse.of(fileUrl, fileMetaData);
    }

    @Override
    public ConfirmUploadResponse confirmUpload(String fileKey, String fileCode) throws FileMetaDataNotFoundException {
        FileMetaData metaData = fileMetaDataService.markReady(fileKey, fileCode);
        return ConfirmUploadResponse.of(metaData);
    }

    @Override
    public GenerateDownloadLinkResponse generateDownloadLink(String fileCode) throws FileMetaDataNotFoundException {
        FileMetaData fileMetaData = fileMetaDataService.getForDownload(fileCode);
        String fileUrl = fileStorageService.generateDownloadLink(fileMetaData.getFileKey());
        return GenerateDownloadLinkResponse.of(fileUrl, fileMetaData);
    }
}
