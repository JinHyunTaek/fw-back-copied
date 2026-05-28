package my.mma.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email
        String email,
        @NotBlank @Size(min = 6, max = 20) String password,
        @NotBlank String resetToken) {
}
