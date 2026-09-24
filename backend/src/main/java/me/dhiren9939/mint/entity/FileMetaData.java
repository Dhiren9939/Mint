package me.dhiren9939.mint.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.dhiren9939.mint.entity.converter.EpochSecondLocalDateTimeConverter;
import me.dhiren9939.mint.service.ExpiryDuration;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDateTime;

@Getter
@Setter
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class FileMetaData {
    private String fileCode;

    private String fileKey;

    private LocalDateTime cleanAt;

    private FileState fileState = FileState.PENDING;

    private ExpiryDuration fileExpiryDuration;

    @DynamoDbPartitionKey
    public String getFileCode(){
        return this.fileCode;
    }

    @DynamoDbConvertedBy(EpochSecondLocalDateTimeConverter.class)
    public LocalDateTime getCleanAt() {
        return this.cleanAt;
    }
}
