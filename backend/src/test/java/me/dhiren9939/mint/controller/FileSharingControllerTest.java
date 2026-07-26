package me.dhiren9939.mint.controller;

import me.dhiren9939.mint.controller.advice.GlobalExceptionHandler;
import me.dhiren9939.mint.dto.response.ConfirmUploadResponse;
import me.dhiren9939.mint.dto.response.GenerateDownloadLinkResponse;
import me.dhiren9939.mint.dto.response.GenerateUploadLinkResponse;
import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.metadata.FileState;
import me.dhiren9939.mint.service.ExpiryDuration;
import me.dhiren9939.mint.service.FileSharingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileSharingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileSharingService fileSharingService;

    @InjectMocks
    private FileSharingController fileSharingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileSharingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/file/upload - 201 Created on valid request")
    void uploadLink_returns201OnSuccess() throws Exception {
        GenerateUploadLinkResponse response = new GenerateUploadLinkResponse(
                "http://s3.upload.url", "a1b2c3", "uploads/key.txt", LocalDateTime.now().plusMinutes(5), 5, FileState.PENDING
        );

        when(fileSharingService.generateUploadLink(eq(ExpiryDuration.MINUTES15), eq(5), eq("test.txt"), eq("text/plain"), eq(1024)))
                .thenReturn(response);

        String jsonBody = """
                {
                    "expiryDuration": "MINUTES15",
                    "maxDownloadCount": 5,
                    "fileName": "test.txt",
                    "contentType": "text/plain",
                    "contentSize": 1024
                }
                """;

        mockMvc.perform(post("/api/v1/file/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileCode").value("a1b2c3"))
                .andExpect(jsonPath("$.data.fileUrl").value("http://s3.upload.url"));
    }

    @Test
    @DisplayName("POST /api/v1/file/upload - 400 Bad Request on missing required fields")
    void uploadLink_returns400OnMissingFields() throws Exception {
        String jsonBody = "{}";

        mockMvc.perform(post("/api/v1/file/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/file/upload - 400 Bad Request on invalid ExpiryDuration")
    void uploadLink_returns400OnInvalidExpiryDuration() throws Exception {
        String jsonBody = """
                {
                    "expiryDuration": "INVALID_DURATION",
                    "maxDownloadCount": 5,
                    "fileName": "test.txt",
                    "contentType": "text/plain",
                    "contentSize": 1024
                }
                """;

        mockMvc.perform(post("/api/v1/file/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/file/upload - 400 Bad Request when content size exceeds 5MB")
    void uploadLink_returns400OnOversizedContent() throws Exception {
        int oversized = 6 * 1024 * 1024;
        String jsonBody = String.format("""
                {
                    "expiryDuration": "MINUTES15",
                    "maxDownloadCount": 5,
                    "fileName": "large.zip",
                    "contentType": "application/zip",
                    "contentSize": %d
                }
                """, oversized);

        mockMvc.perform(post("/api/v1/file/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/file - 200 OK on confirm upload success")
    void confirmUpload_returns200OnSuccess() throws Exception {
        ConfirmUploadResponse response = new ConfirmUploadResponse(
                "a1b2c3", LocalDateTime.now().plusHours(1), 5, FileState.READY
        );

        when(fileSharingService.confirmUpload("uploads/12345678-1234-1234-1234-1234567890ab.txt", "a1b2c3"))
                .thenReturn(response);

        String jsonBody = """
                {
                    "fileKey": "uploads/12345678-1234-1234-1234-1234567890ab.txt",
                    "fileCode": "a1b2c3"
                }
                """;

        mockMvc.perform(post("/api/v1/file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileState").value("READY"));
    }

    @Test
    @DisplayName("POST /api/v1/file - 400 Bad Request when fileCode format is invalid")
    void confirmUpload_returns400OnInvalidFileCode() throws Exception {
        String jsonBody = """
                {
                    "fileKey": "uploads/12345678-1234-1234-1234-1234567890ab.txt",
                    "fileCode": "INVALID!"
                }
                """;

        mockMvc.perform(post("/api/v1/file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/file - 404 Not Found when metadata does not exist")
    void confirmUpload_returns404WhenNotFound() throws Exception {
        when(fileSharingService.confirmUpload(anyString(), anyString()))
                .thenThrow(new FileMetaDataNotFoundException("Invalid metadata information. File not Found."));

        String jsonBody = """
                {
                    "fileKey": "uploads/12345678-1234-1234-1234-1234567890ab.txt",
                    "fileCode": "a1b2c3"
                }
                """;

        mockMvc.perform(post("/api/v1/file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE_CODE"));
    }

    @Test
    @DisplayName("GET /api/v1/file/{fileCode} - 200 OK on valid code")
    void downloadLink_returns200OnSuccess() throws Exception {
        GenerateDownloadLinkResponse response = new GenerateDownloadLinkResponse(
                "http://s3.download.url", LocalDateTime.now().plusHours(1), 1, 5
        );

        when(fileSharingService.generateDownloadLink("a1b2c3")).thenReturn(response);

        mockMvc.perform(get("/api/v1/file/a1b2c3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileUrl").value("http://s3.download.url"));
    }

    @Test
    @DisplayName("GET /api/v1/file/{fileCode} - 404 Not Found when file not found")
    void downloadLink_returns404WhenNotFound() throws Exception {
        when(fileSharingService.generateDownloadLink("a1b2c3"))
                .thenThrow(new FileMetaDataNotFoundException());

        mockMvc.perform(get("/api/v1/file/a1b2c3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE_CODE"));
    }
}
