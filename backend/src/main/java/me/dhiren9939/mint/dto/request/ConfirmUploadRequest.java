package me.dhiren9939.mint.dto.request;

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
public class ConfirmUploadRequest {
    @NotNull
    @Length(min = 36, max = 50, message = "File key must be 36-50 characters long.")
    String fileKey;

    @NotNull(message = "File code is required.")
    @Pattern(regexp = "^[0-9a-z.]{6}$", message = "Must be a valid code.")
    String fileCode;
}
