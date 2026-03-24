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
public class ConfirmUploadResponse {
    private String fileCode;
    private LocalDateTime expiresAt;
    private int maxDownloadCount;
    private FileState fileState;

    public ConfirmUploadResponse(FileMetaData fileMetaData) {
        this.fileCode = fileMetaData.getFileCode();
        this.expiresAt = fileMetaData.getExpiresAt();
        this.maxDownloadCount = fileMetaData.getMaxDownloadCount();
        this.fileState = fileMetaData.getFileState();
    }
}
