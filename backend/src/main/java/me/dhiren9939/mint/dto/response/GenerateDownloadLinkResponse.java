package me.dhiren9939.mint.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import me.dhiren9939.mint.model.entity.metadata.FileMetaData;

import java.time.LocalDateTime;

@Schema(description = "Response containing the temporary S3 download URL and file usage stats.")
public record GenerateDownloadLinkResponse(

        @Schema(
                description = "The pre-signed S3 GET URL. This link is temporary and typically expires in 60 seconds.",
                example = "https://mint-bucket.s3.amazonaws.com/uploads/x7j2k9?X-Amz-Algorithm=AWS4-HMAC-SHA256..."
        )
        String fileUrl,

        @Schema(
                description = "The timestamp when the file record itself will be purged from the system.",
                example = "2026-03-30T14:30:00"
        )
        LocalDateTime expiresAt,

        @Schema(description = "The number of times this file has already been downloaded.", example = "2")
        int downloadCount,

        @Schema(description = "The maximum number of downloads allowed for this file.", example = "10")
        int maxDownloadCount) {

    public static GenerateDownloadLinkResponse of(String fileUrl, FileMetaData fileMetaData){
        return new GenerateDownloadLinkResponse(
                fileUrl,
                fileMetaData.getCleanAt(),
                fileMetaData.getDownloadCount(),
                fileMetaData.getMaxDownloadCount()
        );
    }
}