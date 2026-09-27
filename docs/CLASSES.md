# Backend class hierarchy

Package root: `me.dhiren9939.mint`. Classes marked **planned** don't exist yet; the rest match the code.

## Request path and services

```mermaid
classDiagram
    direction TB

    class RateLimitFilter {
        <<OncePerRequestFilter>>
    }
    class RedisProxyManagerProvider {
        <<AutoCloseable>>
        +get() Optional~ProxyManager~
    }
    class FileSharingController
    class FileSharingService {
        <<interface>>
    }
    class S3SharingService
    class FileStorageService {
        <<interface>>
    }
    class S3FileStorageService
    class CodeGeneratorService
    class FileMetaDataService

    RateLimitFilter --> RedisProxyManagerProvider : fails open when empty
    FileSharingController --> FileSharingService
    FileSharingService <|.. S3SharingService
    S3SharingService --> FileStorageService
    S3SharingService --> CodeGeneratorService
    S3SharingService --> FileMetaDataService
    FileStorageService <|.. S3FileStorageService
```

## Metadata storage with write-through cache

```mermaid
classDiagram
    direction TB

    class FileMetaDataRepository {
        <<interface>>
        +save(FileMetaData) FileMetaData
        +findByFileCode(String) Optional~FileMetaData~
        +findByFileKeyAndFileCode(String, String) Optional~FileMetaData~
        +isFileCodeFree(String) boolean
    }
    class DynamoFileMetaDataRepository {
        <<Repository>>
    }
    class FileMetaDataStore {
        <<planned, Primary>>
        write-through, fails open
    }
    class FileMetaDataCache {
        <<interface, planned>>
        +get(String) Optional~FileMetaData~
        +put(FileMetaData)
        +evict(String)
    }
    class RedisFileMetaDataCache {
        <<planned>>
    }
    class NoOpFileMetaDataCache {
        <<planned, mint.cache.enabled=false>>
    }

    CodeGeneratorService --> FileMetaDataRepository
    FileMetaDataService --> FileMetaDataRepository
    FileMetaDataRepository <|.. DynamoFileMetaDataRepository
    FileMetaDataRepository <|.. FileMetaDataStore
    FileMetaDataStore --> DynamoFileMetaDataRepository : delegate (Qualifier)
    FileMetaDataStore --> FileMetaDataCache
    FileMetaDataCache <|.. RedisFileMetaDataCache
    FileMetaDataCache <|.. NoOpFileMetaDataCache
```

The store is `@Primary`, so the services receive it wherever they inject `FileMetaDataRepository`. Today `CachedFileMetaDataRepository` is a pass-through placeholder for the store.

Write-through rules:

- `save` writes to Dynamo first, then sets the cache entry with `EXAT cleanAt`; if `cleanAt` has already passed, it deletes the key instead.
- If the cache write fails, the store tries a `DEL`, logs, and never fails the request.
- Reads check the cache first and fall back to Dynamo on a miss. Reads never fill the cache, and "not found" is never cached.

## Domain, DTOs and errors

```mermaid
classDiagram
    direction LR

    class FileMetaData {
        <<DynamoDbBean>>
        fileCode : partition key
        fileKey
        cleanAt
        fileState
        fileExpiryDuration
    }
    class FileState {
        <<enum>>
        PENDING
        READY
        DELETED
    }
    class ExpiryDuration {
        <<enum>>
        MINUTES15
        MINUTES30
        MINUTES60
        HOURS24
    }
    class EpochSecondLocalDateTimeConverter {
        <<AttributeConverter>>
    }
    class FileMetaDataBuilder

    FileMetaData --> FileState
    FileMetaData --> ExpiryDuration
    FileMetaData ..> EpochSecondLocalDateTimeConverter : cleanAt
    FileMetaDataBuilder ..> FileMetaData : builds

    class ConfirmUploadRequest
    class GenerateUploadLinkRequest
    class ConfirmUploadResponse {
        <<record>>
    }
    class GenerateUploadLinkResponse {
        <<record>>
    }
    class GenerateDownloadLinkResponse {
        <<record>>
    }
    class ApiResponse~T~ {
        <<record>>
    }
    class ApiError~T~ {
        <<record>>
    }
    class EnumValidator
    GenerateUploadLinkRequest ..> EnumValidator : validated by EnumValue

    class RuntimeException
    class FileCodeGenerationFailure
    class FileMetaDataNotFoundException
    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
    }
    RuntimeException <|-- FileCodeGenerationFailure
    RuntimeException <|-- FileMetaDataNotFoundException
    GlobalExceptionHandler ..> ApiResponse
    GlobalExceptionHandler ..> ApiError
```

## Configuration beans

| Config | Provides |
| --- | --- |
| `AwsConfig` | `S3Client`, `S3Presigner`, `DynamoDbClient`, `DynamoDbEnhancedClient` |
| `RateLimitConfig` | `RedisClient`, `RedisProxyManagerProvider` |
| `SecurityConfig` | CORS source, security filter chain, `RateLimitFilter` registration |
| `OpenApiConfig` | OpenAPI metadata |
