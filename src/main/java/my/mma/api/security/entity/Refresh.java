package my.mma.api.security.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Duration;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@RedisHash(value = "refresh_token")
public class Refresh {

    @Id
    private String token;

    private String email;

    @TimeToLive
    private long expiration;

    public static Refresh of(String email, String token, Long expireMs) {
        return Refresh.builder()
                .email(email)
                .token(token)
                .expiration(Duration.ofMillis(expireMs).toSeconds())
                .build();
    }

}
