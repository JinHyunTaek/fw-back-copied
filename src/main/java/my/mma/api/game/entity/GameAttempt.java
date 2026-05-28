package my.mma.api.game.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * 자정에 초기화 (모든 GameAttempt는 expiration에 따라 자정에 redis에서 삭제됨)
 */

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@RedisHash(value = "game_attempt")
public class GameAttempt {

    @Id
    private Long userId;

    private int count;

    private int adCount;

    @TimeToLive
    private long expiration;

}
