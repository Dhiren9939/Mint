package me.dhiren9939.mint.service;

public interface FileStorageService {
    String generatePreSignedURL();
    void deleteFile(String fileUrl);
}
