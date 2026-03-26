package me.dhiren9939.mint.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.common.ApiResponse;
import me.dhiren9939.mint.dto.request.ConfirmUploadRequest;
import me.dhiren9939.mint.dto.request.GenerateUploadLinkRequest;
import me.dhiren9939.mint.dto.response.ConfirmUploadResponse;
import me.dhiren9939.mint.dto.response.GenerateDownloadLinkResponse;
import me.dhiren9939.mint.dto.response.GenerateUploadLinkResponse;
import me.dhiren9939.mint.service.ExpiryDuration;
import me.dhiren9939.mint.service.FileSharingService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class FileSharingController {
    private final FileSharingService fileSharingService;

    @PostMapping("/file/upload")
    public ResponseEntity<ApiResponse<GenerateUploadLinkResponse>>
    handleGenerateUploadLink(@Valid @RequestBody GenerateUploadLinkRequest dto) {
        GenerateUploadLinkResponse uploadLink = fileSharingService.generateUploadLink(
                ExpiryDuration.valueOf(dto.getExpiryDuration()),
                dto.getMaxDownload(),
                dto.getFileName(),
                dto.getContentType(),
                dto.getContentSize()
        );

        return ApiResponse.of(uploadLink).toResponseEntity(201);
    }

    @PostMapping("/file")
    public ResponseEntity<ApiResponse<ConfirmUploadResponse>> handleConfirmUpload(@Valid @RequestBody ConfirmUploadRequest dto) {
        ConfirmUploadResponse confirmUpload = fileSharingService.confirmUpload(dto.getFileKey(), dto.getFileCode());
        return ApiResponse.of(confirmUpload).toResponseEntity(200);
    }

    @GetMapping("/file/{fileCode}")
    public ResponseEntity<ApiResponse<GenerateDownloadLinkResponse>> handleDownloadLink(
            @NotNull(message = "File code is required.")
            @Pattern(regexp = "^[0-9a-z.]{6}$", message = "Must be a valid code.")
            @PathVariable String fileCode) {

        GenerateDownloadLinkResponse downloadLink = fileSharingService.generateDownloadLink(fileCode);

        return ApiResponse.of(downloadLink).toResponseEntity(200);
    }
}
