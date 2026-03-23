package me.dhiren9939.mint.service;

import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.model.entity.FileState;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class S3SharingServiceTest {

    @Mock
    FileStorageService fileStorageService;

    @Mock
    FileMetaDataRepository fileMetaDataRepository;

    @Mock
    CodeGeneratorService generatorService;

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
        void shouldGenerateUploadLink(ExpiryDuration expiryDuration, int maxDownload) {
            String mockFileCode = "mock12";
            String mockUrl = "https://mockS3Url.com/";

            when(generatorService.getRandomCode()).thenReturn(mockFileCode);
            when(fileStorageService.generatePreSignedURL()).thenReturn(mockUrl);
            when(fileMetaDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            FileMetaData result = s3SharingService.generateUploadLink(expiryDuration, maxDownload);

            verify(generatorService).getRandomCode();
            verify(fileStorageService).generatePreSignedURL();
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

        static Stream<Arguments> confirmUploadParams() {
            return Stream.of(
                    Arguments.of(0, "abcdef", "https://aws.com/s3/abcdef"),
                    Arguments.of(1, "ghijkl", "https://aws.com/s3/ghijkl"));
        }

        @MethodSource("confirmUploadParams")
        @ParameterizedTest(name = "fileMetaDataId{0}, fileCode{1}, fileUrl{3}")
        @DisplayName("Should confirm uploads correctly")
        void shouldConfirmUpload(long fileMetaDataId, String fileCode, String fileUrl) {

        }
    }
}
