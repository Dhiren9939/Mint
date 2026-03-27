package me.dhiren9939.mint.model.entity.metadata;

import me.dhiren9939.mint.service.ExpiryDuration;

import java.time.LocalDateTime;

public class FileMetaDataBuilder {
    private long id = 0;
    private String fileCode;
    private String fileKey;
    private LocalDateTime cleanAt;
    private int downloadCount = 0;
    private int maxDownloadCount = 100;
    private FileState fileState = FileState.PENDING;
    private ExpiryDuration fileExpiryDuration;

    public static FileMetaDataBuilder builder() {
        return new FileMetaDataBuilder();
    }

    public FileMetaDataBuilder id(long id) {
        this.id = id;
        return this;
    }

    public FileMetaDataBuilder fileCode(String fileCode) {
        this.fileCode = fileCode;
        return this;
    }

    public FileMetaDataBuilder fileKey(String fileKey) {
        this.fileKey = fileKey;
        return this;
    }

    public FileMetaDataBuilder cleanAt(LocalDateTime cleanAt) {
        this.cleanAt = cleanAt;
        return this;
    }

    public FileMetaDataBuilder downloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
        return this;
    }

    public FileMetaDataBuilder maxDownloadCount(int maxDownloadCount) {
        this.maxDownloadCount = maxDownloadCount;
        return this;
    }

    public FileMetaDataBuilder fileState(FileState fileState) {
        this.fileState = fileState;
        return this;
    }

    public FileMetaDataBuilder fileExpiryDuration(ExpiryDuration fileExpiryDuration) {
        this.fileExpiryDuration = fileExpiryDuration;
        return this;
    }

    public FileMetaData build() {
        if (fileCode == null)
            throw new IllegalStateException("Incomplete FileMetaData: missing fileCode");
        if (fileKey == null)
            throw new IllegalStateException("Incomplete FileMetaData: missing fileKey");
        if (cleanAt == null)
            throw new IllegalStateException("Incomplete FileMetaData: missing cleanAt");
        if (fileState == null)
            throw new IllegalStateException("Incomplete FileMetaData: missing fileState");
        if (fileExpiryDuration == null)
            throw new IllegalStateException("Incomplete FileMetaData: missing fileExpiryDuration");

        return new FileMetaData(id,
                fileCode,
                fileKey,
                cleanAt,
                downloadCount,
                maxDownloadCount,
                fileState,
                fileExpiryDuration);
    }
}
