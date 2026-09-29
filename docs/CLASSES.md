# Backend class hierarchy

Package root: `me.dhiren9939.mint`.

## Request path and services

```mermaid
classDiagram
    direction TB

    class WebFilterConfig {
        <<Configuration>>
    }
    class CorsFilter {
        <<dev only>>
    }
    class RateLimitFilter {
        <<OncePerRequestFilter>>
    }
    class RedisConnectionProvider {
        <<AutoCloseable>>
        +getConnection() Optional~StatefulRedisConnection~
        +getProxyManager() Optional~ProxyManager~
        +getHost() String
    }
    class RedisHealthIndicator {
        <<HealthIndicator>>
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

    WebFilterConfig ..> CorsFilter : registers first
    WebFilterConfig ..> RateLimitFilter : registers on api
    RateLimitFilter --> RedisConnectionProvider : fail open
    RedisHealthIndicator --> RedisConnectionProvider : always UP
    FileSharingController --> FileSharingService
    FileSharingService <|.. S3SharingService
    S3SharingService --> FileStorageService
    S3SharingService --> CodeGeneratorService
    S3SharingService --> FileMetaDataService
    FileMetaDataService --> FileStorageService : delete on expiry
    FileStorageService <|.. S3FileStorageService
```

Filters are plain servlet filters, there is no Spring Security. Spring Boot's forwarded-headers handling runs first, then CORS (dev profile only, so 429 responses still carry CORS headers), then `RateLimitFilter`, which only covers `/api/*`.

`RedisHealthIndicator` shows up as `redis` in `/actuator/health` on the management port (8081). It reports whether the shared connection is open but is always UP, because the app runs without Redis, and it is not part of the liveness group the load balancer checks.

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
        <<Primary>>
    }
    class FileMetaDataCache {
        <<interface>>
        +get(String) Optional~FileMetaData~
        +put(FileMetaData)
        +evict(String)
    }
    class RedisFileMetaDataCache {
        <<cache enabled>>
    }
    class NoOpFileMetaDataCache {
        <<cache disabled>>
    }
    class RedisConnectionProvider {
        <<AutoCloseable>>
    }

    CodeGeneratorService --> FileMetaDataRepository
    FileMetaDataService --> FileMetaDataRepository
    FileMetaDataRepository <|.. DynamoFileMetaDataRepository
    FileMetaDataRepository <|.. FileMetaDataStore
    FileMetaDataStore --> DynamoFileMetaDataRepository : delegate
    FileMetaDataStore --> FileMetaDataCache
    FileMetaDataCache <|.. RedisFileMetaDataCache
    FileMetaDataCache <|.. NoOpFileMetaDataCache
    RedisFileMetaDataCache --> RedisConnectionProvider : shared connection
```

The store is `@Primary`, so the services receive it wherever they inject `FileMetaDataRepository`. `mint.cache.enabled` (default `true`) picks the Redis or the no-op cache.

Write-through rules:

- `save` writes to Dynamo first, then sets the cache entry with `EXAT min(now + ttl, cleanAt)`. The TTL is `mint.cache.ttl-seconds` (default 300), so a stale entry heals itself quickly; `cleanAt` only caps it. If `cleanAt` has already passed, it deletes the key instead.
- If the cache write fails, the cache tries a `DEL`, logs, and never fails the request.
- Reads check the cache first and fall back to Dynamo on a miss. Reads never fill the cache, and "not found" is never cached.
- `isFileCodeFree` skips the cache and does a consistent read on Dynamo.
- When an expired file is marked `DELETED`, `FileMetaDataService` also deletes the object from S3. A failed delete is logged and not retried.

## Domain, DTOs and errors

```mermaid
classDiagram
    direction LR

    class FileMetaData {
        <<DynamoDbBean>>
        fileCode
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
    FileMetaData ..> EpochSecondLocalDateTimeConverter : cleanAt field
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
| `RateLimitConfig` | `RedisClient`, `RedisConnectionProvider` (one connection shared by the rate limiter and the cache) |
| `WebFilterConfig` | CORS filter (non-prod profiles), `RateLimitFilter` and its registration on `/api/*` |
| `OpenApiConfig` | OpenAPI metadata |
