package me.dhiren9939.mint.service;

import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.model.entity.metadata.FileMetaDataBuilder;
import me.dhiren9939.mint.model.entity.metadata.FileState;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileMetaDataServiceTest {

    @Mock
    private FileMetaDataRepository fileMetaDataRepository;

    @InjectMocks
    private FileMetaDataService fileMetaDataService;

    @Test
    @DisplayName("createPending: saves file metadata with PENDING state and correct cleanAt time")
    void createPending_savesWithCorrectFields() {
        when(fileMetaDataRepository.save(any(FileMetaData.class))).thenAnswer(i -> i.getArgument(0));

        FileMetaData result = fileMetaDataService.createPending("uploads/key.txt", "abc123", ExpiryDuration.MINUTES15, 5);

        assertNotNull(result);
        assertEquals("uploads/key.txt", result.getFileKey());
        assertEquals("abc123", result.getFileCode());
        assertEquals(FileState.PENDING, result.getFileState());
        assertEquals(ExpiryDuration.MINUTES15, result.getFileExpiryDuration());
        assertEquals(5, result.getMaxDownloadCount());
        assertTrue(result.getCleanAt().isAfter(LocalDateTime.now()));

        verify(fileMetaDataRepository, times(1)).save(any(FileMetaData.class));
    }

    @Test
    @DisplayName("markReady: transitions PENDING to READY and updates cleanAt based on ExpiryDuration")
    void markReady_setsReadyStateAndUpdatesExpiry() {
        FileMetaData existing = FileMetaDataBuilder.builder()
                .fileCode("abc123")
                .fileKey("key.txt")
                .cleanAt(LocalDateTime.now().plusMinutes(5))
                .fileState(FileState.PENDING)
                .fileExpiryDuration(ExpiryDuration.HOURS24)
                .maxDownloadCount(10)
                .build();

        when(fileMetaDataRepository.findByFileKeyAndFileCode("key.txt", "abc123"))
                .thenReturn(Optional.of(existing));
        when(fileMetaDataRepository.save(any(FileMetaData.class))).thenAnswer(i -> i.getArgument(0));

        FileMetaData result = fileMetaDataService.markReady("key.txt", "abc123");

        assertEquals(FileState.READY, result.getFileState());
        assertTrue(result.getCleanAt().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    @DisplayName("markReady: throws FileMetaDataNotFoundException when file record not found")
    void markReady_throwsWhenNotFound() {
        when(fileMetaDataRepository.findByFileKeyAndFileCode("key.txt", "abc123"))
                .thenReturn(Optional.empty());

        assertThrows(FileMetaDataNotFoundException.class, () -> fileMetaDataService.markReady("key.txt", "abc123"));
    }

    @Test
    @DisplayName("markReady: marks state DELETED and throws exception if cleanAt is in the past")
    void markReady_marksDeletedWhenExpired() {
        FileMetaData expired = FileMetaDataBuilder.builder()
                .fileCode("abc123")
                .fileKey("key.txt")
                .cleanAt(LocalDateTime.now().minusMinutes(1))
                .fileState(FileState.PENDING)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataRepository.findByFileKeyAndFileCode("key.txt", "abc123"))
                .thenReturn(Optional.of(expired));

        assertThrows(FileMetaDataNotFoundException.class, () -> fileMetaDataService.markReady("key.txt", "abc123"));

        ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
        verify(fileMetaDataRepository).save(captor.capture());
        assertEquals(FileState.DELETED, captor.getValue().getFileState());
    }

    @Test
    @DisplayName("getForDownload: increments download count when file is READY and valid")
    void getForDownload_incrementsDownloadCount() {
        FileMetaData valid = FileMetaDataBuilder.builder()
                .fileCode("abc123")
                .fileKey("key.txt")
                .cleanAt(LocalDateTime.now().plusMinutes(10))
                .fileState(FileState.READY)
                .downloadCount(1)
                .maxDownloadCount(5)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataRepository.findByFileCodeWithLock("abc123")).thenReturn(Optional.of(valid));
        when(fileMetaDataRepository.save(any(FileMetaData.class))).thenAnswer(i -> i.getArgument(0));

        FileMetaData result = fileMetaDataService.getForDownload("abc123");

        assertEquals(2, result.getDownloadCount());
    }

    @Test
    @DisplayName("getForDownload: throws FileMetaDataNotFoundException when file code not found")
    void getForDownload_throwsWhenNotFound() {
        when(fileMetaDataRepository.findByFileCodeWithLock("abc123")).thenReturn(Optional.empty());

        assertThrows(FileMetaDataNotFoundException.class, () -> fileMetaDataService.getForDownload("abc123"));
    }

    @Test
    @DisplayName("getForDownload: throws exception when file state is PENDING")
    void getForDownload_throwsWhenPending() {
        FileMetaData pending = FileMetaDataBuilder.builder()
                .fileCode("abc123")
                .fileKey("key.txt")
                .cleanAt(LocalDateTime.now().plusMinutes(10))
                .fileState(FileState.PENDING)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataRepository.findByFileCodeWithLock("abc123")).thenReturn(Optional.of(pending));

        FileMetaDataNotFoundException ex = assertThrows(FileMetaDataNotFoundException.class, () -> fileMetaDataService.getForDownload("abc123"));
        assertTrue(ex.getMessage().contains("File not uploaded"));
    }

    @Test
    @DisplayName("getForDownload: throws exception when file state is DELETED")
    void getForDownload_throwsWhenDeleted() {
        FileMetaData deleted = FileMetaDataBuilder.builder()
                .fileCode("abc123")
                .fileKey("key.txt")
                .cleanAt(LocalDateTime.now().plusMinutes(10))
                .fileState(FileState.DELETED)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataRepository.findByFileCodeWithLock("abc123")).thenReturn(Optional.of(deleted));

        assertThrows(FileMetaDataNotFoundException.class, () -> fileMetaDataService.getForDownload("abc123"));
    }

    @Test
    @DisplayName("getForDownload: marks state DELETED and throws exception if download limit reached or expired")
    void getForDownload_marksDeletedWhenExpiredOrMaxDownloads() {
        FileMetaData limitReached = FileMetaDataBuilder.builder()
                .fileCode("abc123")
                .fileKey("key.txt")
                .cleanAt(LocalDateTime.now().plusMinutes(10))
                .fileState(FileState.READY)
                .downloadCount(5)
                .maxDownloadCount(5)
                .fileExpiryDuration(ExpiryDuration.MINUTES15)
                .build();

        when(fileMetaDataRepository.findByFileCodeWithLock("abc123")).thenReturn(Optional.of(limitReached));

        assertThrows(FileMetaDataNotFoundException.class, () -> fileMetaDataService.getForDownload("abc123"));

        ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
        verify(fileMetaDataRepository).save(captor.capture());
        assertEquals(FileState.DELETED, captor.getValue().getFileState());
    }
}
