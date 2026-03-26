package me.dhiren9939.mint.dto.response;

import me.dhiren9939.mint.model.entity.FileMetaData;

import java.time.LocalDateTime;

public record GenerateDownloadLinkResponse(
        String fileUrl,
        LocalDateTime expiresAt,
        int downloadCount,
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
