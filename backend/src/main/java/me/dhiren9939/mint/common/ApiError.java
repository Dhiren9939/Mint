package me.dhiren9939.mint.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error structure for all failed API requests.")
public record ApiError<T>(

        @Schema(description = "The HTTP status code.", example = "400")
        int status,

        @Schema(
                description = "A constant machine-readable string code used for programmatic error handling.",
                example = "INVALID_FILE_CODE"
        )
        String code,

        @Schema(description = "A human-readable description of the error.", example = "The provided file name is invalid.")
        String message,

        @Schema(description = "A map of field-specific validation errors, if applicable.")
        Map<String, T> fields
) {
    public static <T> ApiError<T> of(int status, String code, String message) {
        return new ApiError<>(status, code, message, null);
    }

    public static <T> ApiError<T> ofFields(int status, String code, String message, Map<String, T> fields) {
        return new ApiError<>(status, code, message, fields);
    }
}