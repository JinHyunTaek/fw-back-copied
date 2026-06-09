package my.mma.api.game.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameAttempt.of() 팩토리")
class GameAttemptTest {

    @Test
    @DisplayName("of: 다음 자정까지 남은 초로 expiration을 설정하고 초기값을 채운다")
    void of_setsExpirationUntilNextMidnight() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 23, 0, 0); // 23:00

        // when
        GameAttempt attempt = GameAttempt.of(1L, now);

        // then
        assertThat(attempt.getExpiration()).isEqualTo(3600); // 23:00 -> 익일 00:00 = 3600초
        assertThat(attempt.getUserId()).isEqualTo(1L);
        assertThat(attempt.getCount()).isEqualTo(GameAttempt.INITIAL_COUNT);
        assertThat(attempt.getAdCount()).isEqualTo(GameAttempt.INITIAL_AD_COUNT);
    }

    @Test
    @DisplayName("of: 자정 정각에 생성되면 만료까지 86400초(하루)")
    void of_atMidnight_fullDay() {
        // given
        LocalDateTime midnight = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        // when
        GameAttempt attempt = GameAttempt.of(1L, midnight);

        // then
        assertThat(attempt.getExpiration()).isEqualTo(86400);
    }
}
