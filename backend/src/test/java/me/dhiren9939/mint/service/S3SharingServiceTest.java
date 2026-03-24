package me.dhiren9939.mint.service;

import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.model.entity.FileState;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class S3SharingServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileMetaDataRepository fileMetaDataRepository;

    @Mock
    private CodeGeneratorService generatorService;

    @InjectMocks
    private S3SharingService s3SharingService;

    @Nested
    @DisplayName("generateUploadLink Tests")
    class GenerateUploadLinkTest {
        public static Stream<Arguments> uploadLinkParams() {
            return Stream.of(
                    Arguments.of(ExpiryDuration.MINUTES15, 1),
                    Arguments.of(ExpiryDuration.MINUTES15, 2),
                    Arguments.of(ExpiryDuration.MINUTES30, 5),
                    Arguments.of(ExpiryDuration.MINUTES60, 100),
                    Arguments.of(ExpiryDuration.HOURS24, 0));
        }

        @MethodSource("uploadLinkParams")
        @DisplayName("Should Generate Upload Link")
        @ParameterizedTest(name = "expiry={0}, maxDownload={1}")
        public void shouldGenerateUploadLink(ExpiryDuration expiryDuration, int maxDownload) {
            String mockFileCode = "mock12";
            String mockUrl = "https://mockS3Url.com/";

            when(generatorService.getRandomCode()).thenReturn(mockFileCode);
            when(fileStorageService.generateUploadLink()).thenReturn(mockUrl);
            when(fileMetaDataRepository.save(any())).thenAnswer(
                    invocation -> invocation.getArgument(0));

            FileMetaData result = s3SharingService.generateUploadLink(expiryDuration, maxDownload);

            verify(generatorService).getRandomCode();
            verify(fileStorageService).generateUploadLink();
            verify(fileMetaDataRepository).save(any());

            assertNotNull(result);
            assertEquals(mockUrl, result.getFileUrl());
            assertEquals(mockFileCode, result.getFileCode());
            assertEquals(maxDownload, result.getMaxDownloadCount());
            assertEquals(0, result.getDownloadCount());
            assertEquals(FileState.PENDING, result.getFileState());
        }
    }


    @Nested
    @DisplayName("confirmUpload Test")
    class ConfirmUploadTest {
        @Test
        @DisplayName("Should confirm uploads correctly")
        public void shouldConfirmUpload() {
            long fileMetaDataId = 0;
            String fileCode = "abcdef";
            String fileUrl = "https://aws.com/s3/abcdef";

            Answer<Optional<FileMetaData>> returnFileData = (InvocationOnMock invocation) -> {
                long id = invocation.getArgument(0);
                String iFileCode = invocation.getArgument(1);
                String iFileUrl = invocation.getArgument(2);
                return Optional.of(new FileMetaData(id, iFileCode, iFileUrl,
                        LocalDateTime.now(), 0, 10, FileState.PENDING));
            };

            when(fileMetaDataRepository.findByIdAndFileCodeAndFileUrl(any(Long.class), any(String.class), any(String.class)))
                    .thenAnswer(returnFileData);
            when(fileMetaDataRepository.save(any(FileMetaData.class))).thenAnswer(invocation -> invocation.getArgument(0));

            FileMetaData result = s3SharingService.confirmUpload(fileMetaDataId, fileCode, fileUrl);

            verify(fileMetaDataRepository).findByIdAndFileCodeAndFileUrl(any(Long.class), any(String.class), any(String.class));
            verify(fileMetaDataRepository).save(any(FileMetaData.class));

            assertEquals(FileState.READY, result.getFileState());
        }

        @Test
        @DisplayName("Should throw a FileMetaDataNotFoundException")
        public void shouldThrowException() {
            long fileMetaDataId = 0;
            String fileCode = "abcdef";
            String fileUrl = "https://aws.com/s3/abcdef";

            when(fileMetaDataRepository.findByIdAndFileCodeAndFileUrl(any(Long.class), any(String.class), any(String.class)))
                    .thenReturn(Optional.empty());

            assertThrows(FileMetaDataNotFoundException.class,
                    () -> {
                        s3SharingService.confirmUpload(fileMetaDataId, fileCode, fileUrl);
                    });

            verify(fileMetaDataRepository).findByIdAndFileCodeAndFileUrl(any(Long.class), any(String.class), any(String.class));
        }
    }
}
