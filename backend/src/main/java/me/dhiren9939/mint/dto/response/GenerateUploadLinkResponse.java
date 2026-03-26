package me.dhiren9939.mint.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.model.entity.FileState;

import java.time.LocalDateTime;

public record GenerateUploadLinkResponse(
        String fileUrl,
        String fileCode,
        String fileKey,
        LocalDateTime expiresAt,
        int maxDownloadCount,
        FileState fileState) {


    public static GenerateUploadLinkResponse of(String fileUrl, FileMetaData fileMetaData) {
        return new GenerateUploadLinkResponse(
                fileUrl,
                fileMetaData.getFileCode(),
                fileMetaData.getFileKey(),
                fileMetaData.getExpiresAt(),
                fileMetaData.getMaxDownloadCount(),
                fileMetaData.getFileState()
        );
    }
}
