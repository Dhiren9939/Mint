package me.dhiren9939.mint.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.dto.request.ConfirmUploadRequest;
import me.dhiren9939.mint.dto.request.GenerateUploadLinkRequest;
import me.dhiren9939.mint.dto.response.ConfirmUploadResponse;
import me.dhiren9939.mint.dto.response.GenerateUploadLinkResponse;
import me.dhiren9939.mint.dto.response.GetDownloadLinkResponse;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.service.FileSharingService;
import org.springframework.data.util.Pair;
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
    public GenerateUploadLinkResponse handleGenerateUploadLink(@Valid @RequestBody GenerateUploadLinkRequest dto) {
        Pair<String, FileMetaData> fileMetaDataPair = fileSharingService.generateUploadLink(
                dto.getExpiryDuration(),
                dto.getMaxDownload(),
                dto.getFileName(),
                dto.getContentType(),
                dto.getContentSize()
        );

        return new GenerateUploadLinkResponse(fileMetaDataPair.getFirst(), fileMetaDataPair.getSecond());
    }

    @PostMapping("/file")
    public ConfirmUploadResponse handleConfirmUpload(@Valid @RequestBody ConfirmUploadRequest dto) {
        FileMetaData fileMetaData = fileSharingService.confirmUpload(dto.getFileKey(), dto.getFileCode());
        return new ConfirmUploadResponse(fileMetaData);
    }

    @GetMapping("/file/{fileCode}")
    public GetDownloadLinkResponse handleDownloadLink(
            @NotNull(message = "File code is required.")
            @Pattern(regexp = "^[0-9a-z.]{6}$", message = "Must be a valid code.")
            @PathVariable String fileCode) {

        Pair<String, FileMetaData> pair = fileSharingService.getDownloadLink(fileCode);

        return new GetDownloadLinkResponse(pair.getFirst(), pair.getSecond());
    }
}
