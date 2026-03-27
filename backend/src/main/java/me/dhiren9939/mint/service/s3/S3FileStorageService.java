package me.dhiren9939.mint.service.s3;

import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Slf4j
@Service
public class S3FileStorageService implements FileStorageService {

    @Value("${aws.bucket.name}")
    private String bucketName;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3FileStorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public String generateUploadLink(String key, String contentType, int contentSize) {
        PresignedPutObjectRequest putObjectRequest = s3Presigner
                .presignPutObject(r -> r
                        .putObjectRequest(p -> p
                                .bucket(bucketName)
                                .key(key)
                                .contentType(contentType)
                                .contentLength((long) contentSize).build())
                        .signatureDuration(Duration.ofMinutes(1))
                        .build());

        return putObjectRequest.url().toString();
    }

    public String generateDownloadLink(String key) {
        PresignedGetObjectRequest getObjectRequest = s3Presigner.presignGetObject(r -> r
                .getObjectRequest(p -> p
                        .bucket(bucketName)
                        .key(key))
                .signatureDuration(Duration.ofMinutes(1)).build());

        return getObjectRequest.url().toString();
    }

    public void deleteFile(String fileKey) {

    }
}
