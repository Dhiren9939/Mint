package me.dhiren9939.mint.service;

import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.exception.FileCodeGenerationFailure;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(fileMetaDataRepository.isFileCodeFree(any(String.class))).thenReturn(true);

        String result = codeGeneratorService.getUniqueFileCode();
        assertEquals(6, result.length());
    }

    @Test
    @DisplayName("Should retry on collision and return code")
    public void shouldRetryOnCollisionAndReturnCode() {
        when(fileMetaDataRepository.isFileCodeFree(any(String.class))).thenReturn(false, true);

        String result = codeGeneratorService.getUniqueFileCode();

        verify(fileMetaDataRepository, times(2)).isFileCodeFree(any(String.class));

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw an exception back")
    public void shouldThrowAnExceptionBack() {
        when(fileMetaDataRepository.isFileCodeFree(any(String.class))).thenReturn(false, false);

        assertThrows(FileCodeGenerationFailure.class, () -> {
            codeGeneratorService.getUniqueFileCode();
        });

        verify(fileMetaDataRepository, times(2)).isFileCodeFree(any(String.class));
    }
}
