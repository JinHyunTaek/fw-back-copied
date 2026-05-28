package my.mma.api.security.entity;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Builder
@RedisHash(value = "password_reset_token", timeToLive = 300)
public record PasswordResetToken(@Id String token, String email) {
}
