package me.dhiren9939.mint.service;

import me.dhiren9939.mint.exception.FileMetaDataNotFoundException;
import me.dhiren9939.mint.model.entity.FileMetaData;
import org.springframework.data.util.Pair;

public interface FileSharingService {
    Pair<String, FileMetaData> generateUploadLink(ExpiryDuration duration, int maxDownLoad, String fileName, String contentType, int contentSize);

    FileMetaData confirmUpload(String key, String fileCode) throws FileMetaDataNotFoundException;

    Pair<String, FileMetaData> getDownloadLink(String fileCode);
}
