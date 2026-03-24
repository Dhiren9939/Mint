package me.dhiren9939.mint.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.model.entity.FileState;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class GenerateUploadLinkResponse {
    private String fileCode;
    private String fileUrl;
    private String fileKey;
    private LocalDateTime expiresAt;
    private int maxDownloadCount;
    private FileState fileState;

    public GenerateUploadLinkResponse(String fileUrl, FileMetaData fileMetaData) {
        this.fileCode = fileMetaData.getFileCode();
        this.fileUrl = fileUrl;
        this.expiresAt = fileMetaData.getExpiresAt();
        this.maxDownloadCount = fileMetaData.getMaxDownloadCount();
        this.fileState = fileMetaData.getFileState();
        this.fileKey = fileMetaData.getFileKey();
    }
}
