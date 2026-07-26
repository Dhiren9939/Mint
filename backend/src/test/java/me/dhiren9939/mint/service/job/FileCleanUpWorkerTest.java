package me.dhiren9939.mint.service.job;

import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.model.entity.metadata.FileMetaDataBuilder;
import me.dhiren9939.mint.model.entity.metadata.FileState;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import me.dhiren9939.mint.service.ExpiryDuration;
import me.dhiren9939.mint.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileCleanUpWorkerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileMetaDataRepository fileMetaDataRepository;

    @InjectMocks
    private FileCleanUpWorker fileCleanUpWorker;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileCleanUpWorker, "batchSize", 50);
    }

    @Test
    @DisplayName("processBatchCleanUp: deletes DB record and S3 file for non-PENDING state")
    void processBatch_deletesRecordAndS3File() {
        FileMetaData record = FileMetaDataBuilder.builder()
                .fileCode("code12")
                .fileKey("uploads/file.pdf")
                .cleanAt(LocalDateTime.now().minusMinutes(5))
                .fileState(FileState.DELETED)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataRepository.findExpiredOrInState(any(LocalDateTime.class), eq(FileState.DELETED), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(record)));

        boolean result = fileCleanUpWorker.processBatchCleanUp();

        assertTrue(result);
        verify(fileMetaDataRepository).delete(record);
        verify(fileStorageService).deleteFile("uploads/file.pdf");
    }

    @Test
    @DisplayName("processBatchCleanUp: deletes DB record but skips S3 deletion for PENDING files")
    void processBatch_skipsS3DeleteForPending() {
        FileMetaData pendingRecord = FileMetaDataBuilder.builder()
                .fileCode("code12")
                .fileKey("uploads/file.pdf")
                .cleanAt(LocalDateTime.now().minusMinutes(5))
                .fileState(FileState.PENDING)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataRepository.findExpiredOrInState(any(LocalDateTime.class), eq(FileState.DELETED), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(pendingRecord)));

        boolean result = fileCleanUpWorker.processBatchCleanUp();

        assertTrue(result);
        verify(fileMetaDataRepository).delete(pendingRecord);
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("processBatchCleanUp: returns false when no records found")
    void processBatch_returnsFalseWhenEmpty() {
        when(fileMetaDataRepository.findExpiredOrInState(any(LocalDateTime.class), eq(FileState.DELETED), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(Collections.emptyList()));

        boolean result = fileCleanUpWorker.processBatchCleanUp();

        assertFalse(result);
        verify(fileMetaDataRepository, never()).delete(any());
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("processBatchCleanUp: continues processing even if S3 delete throws exception")
    void processBatch_continuesOnS3Error() {
        FileMetaData record1 = FileMetaDataBuilder.builder()
                .fileCode("code01")
                .fileKey("uploads/file1.pdf")
                .cleanAt(LocalDateTime.now().minusMinutes(5))
                .fileState(FileState.READY)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        FileMetaData record2 = FileMetaDataBuilder.builder()
                .fileCode("code02")
                .fileKey("uploads/file2.pdf")
                .cleanAt(LocalDateTime.now().minusMinutes(5))
                .fileState(FileState.READY)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataRepository.findExpiredOrInState(any(LocalDateTime.class), eq(FileState.DELETED), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(record1, record2)));

        doThrow(new RuntimeException("S3 error")).when(fileStorageService).deleteFile("uploads/file1.pdf");

        boolean result = fileCleanUpWorker.processBatchCleanUp();

        assertTrue(result);
        verify(fileMetaDataRepository).delete(record1);
        verify(fileMetaDataRepository).delete(record2);
        verify(fileStorageService).deleteFile("uploads/file2.pdf");
    }
}
