package me.dhiren9939.mint.repository;

import me.dhiren9939.mint.entity.FileMetaData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.Optional;

@Repository
public class DynamoFileMetaDataRepository implements FileMetaDataRepository {

    private final DynamoDbTable<FileMetaData> table;

    public DynamoFileMetaDataRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                         @Value("${aws.dynamodb.table.name}") String tableName) {
        this.table = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(FileMetaData.class));
    }

    @Override
    public FileMetaData save(FileMetaData fileMetaData) {
        table.putItem(fileMetaData);
        return fileMetaData;
    }

    @Override
    public Optional<FileMetaData> findByFileCode(String fileCode) {
        return Optional.ofNullable(table.getItem(consistentGet(fileCode)));
    }

    @Override
    public Optional<FileMetaData> findByFileKeyAndFileCode(String fileKey, String fileCode) {
        return Optional
                .ofNullable(table.getItem(consistentGet(fileCode)))
                .filter(fileMetaData -> fileMetaData.getFileKey().equals(fileKey));
    }

    private GetItemEnhancedRequest consistentGet(String fileCode) {
        return GetItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(fileCode).build())
                .consistentRead(true)
                .build();
    }
}
