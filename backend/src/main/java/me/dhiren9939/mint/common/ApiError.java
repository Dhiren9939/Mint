package me.dhiren9939.mint.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError<T>(
        int status,
        String code,
        String message,
        Map<String, T> fields
) {
    public static <T> ApiError<T> of(int status, String code, String message) {
        return new ApiError<>(status, code, message, null);
    }

    public static <T> ApiError<T> ofFields(int status, String code, String message, Map<String, T> fields) {
        return new ApiError<>(status, code, message, fields);
    }
}