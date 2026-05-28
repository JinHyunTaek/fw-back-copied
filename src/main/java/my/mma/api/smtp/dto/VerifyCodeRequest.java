package my.mma.api.smtp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

public record VerifyCodeRequest(@NotBlank @Email String email,
                                @Pattern(regexp = "\\d{6}") @JsonProperty("verifyingCode") String code
) {
}