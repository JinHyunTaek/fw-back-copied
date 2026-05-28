package my.mma.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import my.mma.api.user.entity.User;

import java.time.LocalDateTime;

public record JoinRequest(
        @NotBlank @Email
        String email,
        @NotBlank @Size(min = 2, max = 12) String nickname,
        @NotBlank @Size(min = 6, max = 20) String password) {
        public User toEntity(String encodedPwd){
                return User.builder()
                        .point(User.JOIN_POINT)
                        .role("ROLE_USER")
                        .email(email)
                        .password(encodedPwd)
                        .nickname(nickname)
                        .build();
        }
}
