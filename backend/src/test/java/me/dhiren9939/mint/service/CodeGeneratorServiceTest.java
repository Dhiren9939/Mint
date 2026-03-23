package me.dhiren9939.mint.service;

import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.exception.FileCodeGenerationFailure;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class CodeGeneratorServiceTest {

    @Mock
    FileMetaDataRepository fileMetaDataRepository;

    @InjectMocks
    CodeGeneratorService codeGeneratorService;

    @Test
    @DisplayName("Should return a 6 digit code")
    public void shouldReturnFileCode() {
        String result = codeGeneratorService.getUniqueFileCode();
        assertEquals(6, result.length());
    }

    @Test
    @DisplayName("Should retry on collision and return code")
    public void shouldRetryOnCollisionAndReturnCode() {
        Answer<Optional<FileMetaData>> returnMetaDataWithId = (InvocationOnMock invocation) -> {
            FileMetaData metaData = new FileMetaData();
            metaData.setFileCode(invocation.getArgument(0));
            return Optional.of(metaData);
        };

        when(fileMetaDataRepository.findByFileCode(any(String.class))).thenAnswer(returnMetaDataWithId).thenReturn(Optional.empty());

        String result = codeGeneratorService.getUniqueFileCode();

        verify(fileMetaDataRepository, times(2)).findByFileCode(any(String.class));

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw an exception back")
    public void shouldThrowAnExceptionBack() {
        Answer<Optional<FileMetaData>> returnMetaDataWithId = (InvocationOnMock invocation) -> {
            FileMetaData metaData = new FileMetaData();
            metaData.setFileCode(invocation.getArgument(0));
            return Optional.of(metaData);
        };

        when(fileMetaDataRepository.findByFileCode(any(String.class))).thenAnswer(returnMetaDataWithId).thenAnswer(returnMetaDataWithId);

        assertThrows(FileCodeGenerationFailure.class, () -> {
            codeGeneratorService.getUniqueFileCode();
        });

        verify(fileMetaDataRepository, times(2)).findByFileCode(any(String.class));
    }
}