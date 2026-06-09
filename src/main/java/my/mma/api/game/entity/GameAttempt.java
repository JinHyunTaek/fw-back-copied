package my.mma.api.game.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.Duration;
import java.time.LocalDateTime;

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

    public static final int INITIAL_COUNT = 10;
    public static final int INITIAL_AD_COUNT = 5;

    @Id
    private Long userId;

    private int count;

    private int adCount;

    @TimeToLive
    private long expiration;

    // 자정 만료
    public static GameAttempt of(Long userId, LocalDateTime now) {
        long secondsUntilMidnight = Duration.between(
                now, now.toLocalDate().plusDays(1).atStartOfDay()).getSeconds();
        return GameAttempt.builder()
                .userId(userId)
                .count(INITIAL_COUNT)
                .adCount(INITIAL_AD_COUNT)
                .expiration(secondsUntilMidnight)
                .build();
    }

}