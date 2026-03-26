package me.dhiren9939.mint.dto.response;

import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.model.entity.FileState;

import java.time.LocalDateTime;

public record ConfirmUploadResponse(
        String fileCode,
        LocalDateTime expiresAt,
        int maxDownloadCount,
        FileState fileState) {

    public static ConfirmUploadResponse of(FileMetaData fileMetaData) {
        return new ConfirmUploadResponse(
                fileMetaData.getFileCode(),
                fileMetaData.getCleanAt(),
                fileMetaData.getMaxDownloadCount(),
                fileMetaData.getFileState()
        );
    }
}
