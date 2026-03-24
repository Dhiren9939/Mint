package me.dhiren9939.mint.service;

public interface FileStorageService {
    String generateUploadLink();
    void deleteFile(String fileUrl);
}
