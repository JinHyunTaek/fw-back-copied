package my.mma.api.smtp.entity;

import lombok.*;
import my.mma.api.smtp.constant.JoinCodeConstant;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@RedisHash(value = "join_code")
public class JoinCode {

    @Id
    private String email;

    private String code;

    @TimeToLive
    private long expiration;

    public static JoinCode of(String email, String joinCode){
        return JoinCode.builder()
                .email(email)
                .code(joinCode)
                .expiration(JoinCodeConstant.EXPIRATION_SECONDS.getValue())
                .build();
    }

}
