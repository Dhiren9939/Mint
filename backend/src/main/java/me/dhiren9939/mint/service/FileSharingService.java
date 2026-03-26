package me.dhiren9939.mint.service;

import me.dhiren9939.mint.dto.response.ConfirmUploadResponse;
import me.dhiren9939.mint.dto.response.GenerateUploadLinkResponse;
import me.dhiren9939.mint.dto.response.GenerateDownloadLinkResponse;
import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;

public interface FileSharingService {
    GenerateUploadLinkResponse generateUploadLink(ExpiryDuration duration, int maxDownLoad, String fileName, String contentType, int contentSize);

    ConfirmUploadResponse confirmUpload(String key, String fileCode) throws FileMetaDataNotFoundException;

    GenerateDownloadLinkResponse getDownloadLink(String fileCode);
}
