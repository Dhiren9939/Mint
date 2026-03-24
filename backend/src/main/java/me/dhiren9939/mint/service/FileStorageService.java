package me.dhiren9939.mint.service;

public interface FileStorageService {
    String generateUploadLink(String fileName, String contentType, int contentSize);

    String generateDownloadLink(String key);

    void deleteFile(String fileUrl);
}
