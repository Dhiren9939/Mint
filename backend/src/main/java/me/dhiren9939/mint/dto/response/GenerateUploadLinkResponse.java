package me.dhiren9939.mint.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;
import me.dhiren9939.mint.model.entity.metadata.FileState;

import java.time.LocalDateTime;

@Schema(description = "Response containing the pre-signed S3 URL and file identification metadata.")
public record GenerateUploadLinkResponse(

        @Schema(
                description = "The pre-signed S3 PUT URL where the file should be uploaded. Valid for 60 seconds.",
                example = "https://mint-bucket.s3.amazonaws.com/uploads/uuid-key?X-Amz-Signature=..."
        )
        String fileUrl,

        @Schema(description = "A short 6-character code for user-friendly file sharing.", example = "x7j2k9")
        String fileCode,

        @Schema(
                description = "The unique internal storage key. Required for the /file/confirm endpoint.",
                example = "550e8400-e29b-41d4-a716-446655440000.txt"
        )
        String fileKey,

        @Schema(description = "Timestamp when this file will be automatically purged from storage.",
                example = "2026-03-31T18:00:00")
        LocalDateTime expiresAt,

        @Schema(description = "Maximum allowed downloads for this file.", example = "10")
        int maxDownloadCount,

        @Schema(description = "Initial state of the file record (usually PENDING).", example = "PENDING")
        FileState fileState) {


    public static GenerateUploadLinkResponse of(String fileUrl, FileMetaData fileMetaData) {
        return new GenerateUploadLinkResponse(
                fileUrl,
                fileMetaData.getFileCode(),
                fileMetaData.getFileKey(),
                fileMetaData.getCleanAt(),
                fileMetaData.getMaxDownloadCount(),
                fileMetaData.getFileState()
        );
    }
}