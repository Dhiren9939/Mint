package me.dhiren9939.mint.common;

import java.util.Map;

public record ApiError(
        int status,
        String code,
        String message,
        Map<String, String> fields
) {
    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, null);
    }

    public static ApiError ofFields(int status, String code, String message, Map<String, String> fields) {
        return new ApiError(status, code, message, fields);
    }
}