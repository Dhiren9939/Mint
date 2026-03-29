package me.dhiren9939.mint.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "File Controller", description = "Endpoints for uploading and accessing files.")
public class FileSharingController {
    private final FileSharingService fileSharingService;

    @PostMapping("/file/upload")
    @Operation(summary = "Get an upload link for S3",
            description = "Returns a Pre-signed S3 PUT URL. " +
                    "The URL is signed with file size and content type, so the size and the content type of the file actually " +
                    "being uploaded should be consistent with the fields in this request i.e same Content-Length and " +
                    "Content-Type header for the S3 upload. The link expires in 1 minute.")
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
    @Operation(summary = "Notify the server after upload complete.",
            description = "This endpoint must be called after the upload is complete." +
                    " The server then makes the necessary updates to serve the file. " +
                    "The user gets 5 minutes to make this API call before the file record is discard.")
    public ResponseEntity<ApiResponse<ConfirmUploadResponse>> handleConfirmUpload(@Valid @RequestBody ConfirmUploadRequest dto) {
        ConfirmUploadResponse confirmUpload = fileSharingService.confirmUpload(dto.getFileKey(), dto.getFileCode());
        return ApiResponse.of(confirmUpload).toResponseEntity(200);
    }

    @GetMapping("/file/{fileCode}")
    @Operation(summary = "Get download link.", description = "Returns a S3 GET URL with an expiry of one minute.")
    public ResponseEntity<ApiResponse<GenerateDownloadLinkResponse>> handleDownloadLink(
            @NotNull(message = "File code is required.")
            @Pattern(regexp = "^[0-9a-z.]{6}$", message = "Must be a valid code.")
            @Parameter(description = "The 6-character alphanumeric code for the file.", example = "a1b2c3")
            @PathVariable String fileCode) {

        GenerateDownloadLinkResponse downloadLink = fileSharingService.generateDownloadLink(fileCode);

        return ApiResponse.of(downloadLink).toResponseEntity(200);
    }
}
