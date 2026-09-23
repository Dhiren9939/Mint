package me.dhiren9939.mint.entity;

import me.dhiren9939.mint.service.ExpiryDuration;

import java.time.LocalDateTime;

public class FileMetaDataBuilder {
    private String fileCode;
    private String fileKey;
    private LocalDateTime cleanAt;
    private FileState fileState = FileState.PENDING;
    private ExpiryDuration fileExpiryDuration;

    public static FileMetaDataBuilder builder() {
        return new FileMetaDataBuilder();
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

        return new FileMetaData(fileCode,
                fileKey,
                cleanAt,
                fileState,
                fileExpiryDuration);
    }
}
