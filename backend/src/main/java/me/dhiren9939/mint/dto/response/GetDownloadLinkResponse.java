package me.dhiren9939.mint.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.dhiren9939.mint.model.entity.FileMetaData;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class GetDownloadLinkResponse {
    private String fileUrl;
    private LocalDateTime expiresAt;
    private int downloadCount;
    private int maxDownloadCount;

    public GetDownloadLinkResponse(String fileUrl, FileMetaData fileMetaData) {
        this.fileUrl = fileUrl;
        this.downloadCount = fileMetaData.getDownloadCount();
        this.expiresAt = fileMetaData.getExpiresAt();
        this.maxDownloadCount = fileMetaData.getMaxDownloadCount();
    }
}
