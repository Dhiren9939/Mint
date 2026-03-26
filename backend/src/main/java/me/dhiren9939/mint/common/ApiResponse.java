package me.dhiren9939.mint.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.ResponseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError<?> error
) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> fail(ApiError<?> error) {
        return new ApiResponse<>(false, null, null, error);
    }

    public ResponseEntity<ApiResponse<T>> toResponseEntity() {
        if (error != null)
            return ResponseEntity.status(error.status()).body(this);
        return ResponseEntity.status(200).body(this);
    }

    public ResponseEntity<ApiResponse<T>> toResponseEntity(int statusCode) {
        return ResponseEntity.status(statusCode).body(this);
    }
}