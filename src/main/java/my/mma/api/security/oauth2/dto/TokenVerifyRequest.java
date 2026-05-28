package my.mma.api.security.oauth2.dto;

import jakarta.validation.constraints.NotNull;
import my.mma.api.user.entity.User;

public record TokenVerifyRequest(
        @NotNull String domain,
        @NotNull String accessToken,
        @NotNull String email,
        @NotNull String socialId,
        String fcmToken) {

    public User toEntity() {
        return User.builder()
                .role("ROLE_USER")
                .email(email)
                .point(User.JOIN_POINT)
                .username(domain + "_" + socialId)
                .fcmToken(fcmToken)
                .build();
    }

}
