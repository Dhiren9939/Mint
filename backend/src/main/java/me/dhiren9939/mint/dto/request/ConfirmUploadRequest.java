package me.dhiren9939.mint.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "Request body to confirm a successful file upload to S3. " +
                "This links the physical S3 object to the database record using the generated key and code."
)
public class ConfirmUploadRequest {

    @Schema(
            description = "The unique S3 object key returned during the upload link generation process.",
            example = "a1b2c3d4-e5f6-7890-abcd-1234567890ab.jpg",
            minLength = 36,
            maxLength = 70,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "File key is required.")
    @Length(min = 36, max = 70, message = "File key must be 36-70 characters long.")
    private String fileKey;

    @Schema(
            description = "The 6-character short code used for public access and identification.",
            example = "x7j2k9",
            pattern = "^[0-9a-z.]{6}$",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "File code is required.")
    @Pattern(regexp = "^[0-9a-z.]{6}$", message = "Must be a valid code.")
    private String fileCode;
}