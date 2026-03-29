package me.dhiren9939.mint.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.model.entity.metadata.FileState;

import java.time.LocalDateTime;

@Schema(description = "Response returned after successfully confirming an S3 upload.")
public record ConfirmUploadResponse(

        @Schema(description = "The unique 6-character code used to identify the file.", example = "x7j2k9")
        String fileCode,

        @Schema(description = "The timestamp when the file will be automatically deleted from the server.",
                example = "2024-12-31T23:59:59")
        LocalDateTime expiresAt,

        @Schema(description = "The total number of times this file is allowed to be downloaded.", example = "10")
        int maxDownloadCount,

        @Schema(description = "The current lifecycle state of the file.", example = "READY")
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