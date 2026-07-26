package me.dhiren9939.mint.service.s3;

import me.dhiren9939.mint.dto.response.ConfirmUploadResponse;
import me.dhiren9939.mint.dto.response.GenerateDownloadLinkResponse;
import me.dhiren9939.mint.dto.response.GenerateUploadLinkResponse;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.model.entity.metadata.FileMetaDataBuilder;
import me.dhiren9939.mint.model.entity.metadata.FileState;
import me.dhiren9939.mint.service.CodeGeneratorService;
import me.dhiren9939.mint.service.ExpiryDuration;
import me.dhiren9939.mint.service.FileMetaDataService;
import me.dhiren9939.mint.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3SharingServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CodeGeneratorService codeGeneratorService;

    @Mock
    private FileMetaDataService fileMetaDataService;

    @InjectMocks
    private S3SharingService s3SharingService;

    @Test
    @DisplayName("generateUploadLink: extracts file extension and returns pre-signed upload link DTO")
    void generateUploadLink_returnsResponseWithUrl() {
        when(fileStorageService.generateUploadLink(anyString(), eq("application/pdf"), eq(1024)))
                .thenReturn("http://s3.amazonaws.com/presigned-put-url");
        when(codeGeneratorService.getRandomCode()).thenReturn("code12");

        FileMetaData metaData = FileMetaDataBuilder.builder()
                .fileCode("code12")
                .fileKey("uploads/uuid.pdf")
                .cleanAt(LocalDateTime.now().plusMinutes(5))
                .fileState(FileState.PENDING)
                .maxDownloadCount(5)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataService.createPending(anyString(), eq("code12"), eq(ExpiryDuration.MINUTES15), eq(5)))
                .thenReturn(metaData);

        GenerateUploadLinkResponse response = s3SharingService.generateUploadLink(
                ExpiryDuration.MINUTES15, 5, "report.pdf", "application/pdf", 1024
        );

        assertNotNull(response);
        assertEquals("http://s3.amazonaws.com/presigned-put-url", response.fileUrl());
        assertEquals("code12", response.fileCode());
        verify(fileStorageService).generateUploadLink(argThat(k -> k.endsWith(".pdf")), eq("application/pdf"), eq(1024));
    }

    @Test
    @DisplayName("generateUploadLink: handles file name without extension correctly")
    void generateUploadLink_handlesNoExtension() {
        when(fileStorageService.generateUploadLink(anyString(), anyString(), anyInt()))
                .thenReturn("http://s3.amazonaws.com/upload");
        when(codeGeneratorService.getRandomCode()).thenReturn("code12");

        FileMetaData metaData = FileMetaDataBuilder.builder()
                .fileCode("code12")
                .fileKey("uploads/uuid")
                .cleanAt(LocalDateTime.now().plusMinutes(5))
                .fileState(FileState.PENDING)
                .maxDownloadCount(2)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataService.createPending(anyString(), anyString(), any(), anyInt()))
                .thenReturn(metaData);

        GenerateUploadLinkResponse response = s3SharingService.generateUploadLink(
                ExpiryDuration.MINUTES15, 2, "README", "text/plain", 100
        );

        assertNotNull(response);
        verify(fileStorageService).generateUploadLink(argThat(k -> !k.contains(".")), eq("text/plain"), eq(100));
    }

    @Test
    @DisplayName("confirmUpload: delegates to fileMetaDataService.markReady")
    void confirmUpload_delegatesToMetaDataService() {
        FileMetaData metaData = FileMetaDataBuilder.builder()
                .fileCode("code12")
                .fileKey("key.txt")
                .cleanAt(LocalDateTime.now().plusHours(1))
                .fileState(FileState.READY)
                .maxDownloadCount(10)
                .fileExpiryDuration(ExpiryDuration.MINUTES60)
                .build();

        when(fileMetaDataService.markReady("key.txt", "code12")).thenReturn(metaData);

        ConfirmUploadResponse response = s3SharingService.confirmUpload("key.txt", "code12");

        assertNotNull(response);
        assertEquals("code12", response.fileCode());
        assertEquals(FileState.READY, response.fileState());
        verify(fileMetaDataService).markReady("key.txt", "code12");
    }

    @Test
    @DisplayName("generateDownloadLink: fetches metadata and generates presigned S3 download link")
    void generateDownloadLink_returnsUrlAndMetadata() {
        FileMetaData metaData = FileMetaDataBuilder.builder()
                .fileCode("code12")
                .fileKey("key.txt")
                .cleanAt(LocalDateTime.now().plusHours(1))
                .fileState(FileState.READY)
                .downloadCount(1)
                .maxDownloadCount(5)
                .fileExpiryDuration(ExpiryDuration.MINUTES60)
                .build();

        when(fileMetaDataService.getForDownload("code12")).thenReturn(metaData);
        when(fileStorageService.generateDownloadLink("key.txt")).thenReturn("http://s3.amazonaws.com/get-url");

        GenerateDownloadLinkResponse response = s3SharingService.generateDownloadLink("code12");

        assertNotNull(response);
        assertEquals("http://s3.amazonaws.com/get-url", response.fileUrl());
        assertEquals(1, response.downloadCount());
        assertEquals(5, response.maxDownloadCount());
    }
}
