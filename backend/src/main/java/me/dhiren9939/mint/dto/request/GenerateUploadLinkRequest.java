package me.dhiren9939.mint.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.dhiren9939.mint.common.EnumValue;
import me.dhiren9939.mint.service.ExpiryDuration;
import org.hibernate.validator.constraints.Length;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GenerateUploadLinkRequest {
    private static final int maxContentSize = 100 * 1024;

    @NotNull(message = "Expiry duration is required.")
    @EnumValue(enumClass = ExpiryDuration.class)
    String expiryDuration;

    @NotNull(message = "Max download count is required")
    @Min(value = 1, message = "Max downloads must be at least 1")
    @Max(value = 100, message = "Max downloads cannot exceed 100")
    Integer maxDownload;

    @Pattern(
            regexp = "^[\\w\\-. ]+\\.\\w+$",
            message = "Must have a file extension."
    )
    @NotNull(message = "File name is required")
    @NotBlank(message = "File name cannot be empty.")
    @Length(min = 4, max = 50, message = "File name must be 4-50 characters long.")
    String fileName;

    @NotNull(message = "Content Type is required.")
    @NotBlank(message = "Content Type cannot be empty.")
    @Length(min = 1, max = 20, message = "Content type must be 1-50 characters long.")
    String contentType;

    @NotNull(message = "Content Size is required.")
    @Min(value = 1, message = "File size but be greater the 0 bytes.")
    @Max(value = maxContentSize, message = "File Size cannot not exceed " + maxContentSize + "bytes.")
    Integer contentSize;
}
