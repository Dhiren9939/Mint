package me.dhiren9939.mint.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.dhiren9939.mint.common.EnumValue;
import me.dhiren9939.mint.service.ExpiryDuration;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for generating a pre-signed S3 upload link")
public class GenerateUploadLinkRequest {

    private static final int MAX_CONTENT_SIZE = 1024 * 1024;

    @Schema(
            description = "Duration until the file expires and is deleted",
            example = "MINUTES30",
            allowableValues = {"MINUTES15",
                    "MINUTES30",
                    "MINUTES60",
                    "HOURS24"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Expiry duration is required.")
    @EnumValue(enumClass = ExpiryDuration.class)
    private String expiryDuration;

    @Schema(
            description = "Maximum number of times this file can be downloaded",
            example = "5",
            minimum = "1",
            maximum = "100",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Max download count is required")
    @Min(value = 1, message = "Max downloads must be at least 1")
    @Max(value = 100, message = "Max downloads cannot exceed 100")
    private Integer maxDownloadCount;

    @Schema(
            description = "Name of the file (must include extension)",
            example = "project_specs.pdf",
            minLength = 4,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "File name is required")
    @NotBlank(message = "File name cannot be empty.")
    @Length(min = 4, max = 50, message = "File name must be 4-50 characters long.")
    private String fileName;

    @Schema(
            description = "Standard MIME type of the file",
            example = "application/pdf",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Content Type is required.")
    @NotBlank(message = "Content Type cannot be empty.")
    @Length(min = 1, max = 50, message = "Content type must be 1-50 characters long.")
    private String contentType;

    @Schema(
            description = "Size of the file in bytes. Strictly enforced by S3 signature.",
            example = "524288",
            maximum = "1048576",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Content Size is required.")
    @Min(value = 1, message = "File size must be greater than 0 bytes.")
    @Max(value = MAX_CONTENT_SIZE, message = "File Size cannot exceed 1MB.")
    private Integer contentSize;
}