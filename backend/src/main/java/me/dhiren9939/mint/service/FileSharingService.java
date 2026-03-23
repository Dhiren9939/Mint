package me.dhiren9939.mint.service;

import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.FileMetaData;

public interface FileSharingService {
    FileMetaData generateUploadLink(ExpiryDuration duration, int maxDownLoad);
    FileMetaData confirmUpload(long fileMetaDataId, String fileCode, String fileUrl) throws FileMetaDataNotFoundException;
    FileMetaData getDownloadLink(String fileCode);
}
