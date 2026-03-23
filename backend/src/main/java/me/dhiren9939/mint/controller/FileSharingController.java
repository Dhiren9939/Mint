package me.dhiren9939.mint.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.service.ExpiryDuration;
import me.dhiren9939.mint.service.FileSharingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("/api")
@AllArgsConstructor
public class FileSharingController {
    private final FileSharingService fileSharingService;

    @PostMapping("/file/upload")
    public FileMetaData handleGenerateUploadLink(@RequestBody ExpiryDuration expiryDuration,@RequestBody int maxDownload){
        return fileSharingService.generateUploadLink(expiryDuration,maxDownload);
    }

    @PostMapping("/file")
    public FileMetaData handleConfirmUpload(@RequestBody long id, @RequestBody String fileCode,@RequestBody String fileUrl){
        return fileSharingService.confirmUpload(id,fileCode,fileUrl);
    }

    @GetMapping("/file")
    public FileMetaData handleDownloadLink(@RequestBody String fileCode){
        return fileSharingService.getDownloadLink(fileCode);
    }
}
