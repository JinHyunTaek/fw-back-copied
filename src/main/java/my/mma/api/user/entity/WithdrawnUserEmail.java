package my.mma.api.user.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@RedisHash(value = "withdrawn_user_email")
public class WithdrawnUserEmail {

    @Id
    private String email;

    @TimeToLive
    private long expiration;

}
