package my.mma.api.game.service;

import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.game.dto.GameAttemptResponse;
import my.mma.api.game.entity.GameAttempt;
import my.mma.api.game.repository.GameAttemptRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamePointServiceTest {

    @Mock
    private GameAttemptRepository gameAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GamePointService gamePointService;

    private final String email = "test-email@naver.com";
    private User user;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .email(email)
                .nickname("test-nick")
                .role("ROLE_USER")
                .password("pwd-123")
                .point(500)
                .earnedBetSucceedPoint(1000)
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @Nested
    @DisplayName("GamePointService.updateGameAttemptCount() test")
    class updateGameAttemptCountTest {

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("게임 화면 진입하지도 않은 사용자가 updateGameAttemptCount() 호출 시 400 예외 발생")
        void userIdNotFoundThenThrowException_400(boolean isSubtract) {
            // given
            // when
            when(gameAttemptRepository.findById(user.getId())).thenReturn(Optional.empty());

            // then
            assertThatThrownBy(() ->
                    gamePointService.updateGameAttemptCount(email, isSubtract))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.NO_SUCH_USER_FOUND_400.name());
        }

        @Test
        @DisplayName("gameAttemptCount 없을 때 게임 시작 시도 시 400 예외 발생")
        void noGameAttemptCountWhenAttemptGameStartThenThrowException_400() {
            // given
            GameAttempt gameAttempt = GameAttempt.builder()
                    .count(0)
                    .adCount(5)
                    .userId(user.getId())
                    .build();

            // when
            when(gameAttemptRepository.findById(user.getId())).thenReturn(Optional.of(gameAttempt));

            // then
            assertThatThrownBy(() ->
                    gamePointService.updateGameAttemptCount(email, true))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.GAME_ATTEMPT_COUNT_EXPIRED_400.name());
        }

        @Test
        @DisplayName("보상형 광고 최대 시청 횟수 소진됐을 때 보상형 광고 시청 시도 시 예외 발생")
        void watchAdWhenAdCountExpiredThenThrowException_400() {
            // given
            GameAttempt gameAttempt = GameAttempt.builder()
                    .count(0)
                    .adCount(0)
                    .userId(user.getId())
                    .build();

            // when
            when(gameAttemptRepository.findById(user.getId())).thenReturn(Optional.of(gameAttempt));

            // then
            assertThatThrownBy(() ->
                    gamePointService.updateGameAttemptCount(email, false))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AD_COUNT_EXPIRED_400.name());
        }

        @Test
        @DisplayName("게임 시작 시 gameAttemptCount 1 감소")
        void attemptGameThenDecreaseGameAttemptCount() {
            // given
            int prevGameAttemptCount = 5;
            GameAttempt gameAttempt = GameAttempt.builder()
                    .count(prevGameAttemptCount)
                    .adCount(1)
                    .userId(user.getId())
                    .build();

            // when
            when(gameAttemptRepository.findById(user.getId())).thenReturn(Optional.of(gameAttempt));
            gamePointService.updateGameAttemptCount(email, true);

            // then
            assertThat(gameAttempt.getCount()).isEqualTo(prevGameAttemptCount - 1);
            verify(gameAttemptRepository).save(gameAttempt);
        }

        @Test
        @DisplayName("보상형 광고 시청 시 gameAttemptCount 1 증가 & adCount 1 감소")
        void watchAdThenIncreaseGameAttemptCountAndAdCount() {
            // given
            int prevGameAttemptCount = 5;
            int prevAdCount = 2;
            GameAttempt gameAttempt = GameAttempt.builder()
                    .count(prevGameAttemptCount)
                    .adCount(prevAdCount)
                    .userId(user.getId())
                    .build();

            // when
            when(gameAttemptRepository.findById(user.getId())).thenReturn(Optional.of(gameAttempt));
            gamePointService.updateGameAttemptCount(email, false);

            // then
            assertThat(gameAttempt.getCount()).isEqualTo(prevGameAttemptCount + 1);
            assertThat(gameAttempt.getAdCount()).isEqualTo(prevAdCount - 1);
            verify(gameAttemptRepository).save(gameAttempt);
        }

    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50})
    @DisplayName("게임 종료 시 새로운 포인트로 업데이트")
    void updateWithNewPointWhenGameEnds(int pointToAdd) {
        // given
        int newPoint = user.getPoint() + pointToAdd;

        // when
        gamePointService.updatePoint(email, newPoint);

        // then
        assertThat(user.getPoint()).isEqualTo(newPoint);
    }

    @Test
    @DisplayName("오늘 최초로 게임 메인 화면 진입 시 새로운 GameAttempt 생성")
    void saveGameAttemptWhenEnterMainGameScreen(){
        // when
        when(gameAttemptRepository.findById(user.getId())).thenReturn(Optional.empty());
        GameAttemptResponse resp = gamePointService.getGameAttemptCount(email);

        // then
        assertThat(resp.count()).isEqualTo(GameAttempt.INITIAL_COUNT);
        assertThat(resp.adCount()).isEqualTo(GameAttempt.INITIAL_AD_COUNT);
        verify(gameAttemptRepository).save(any(GameAttempt.class));
    }

    @Test
    @DisplayName("기존 GameAttempt 있으면 그대로 반환하고 새로 저장하지 않음")
    void returnExisting() {
        // given
        int attemptCount = 7;
        int adCount = 2;
        GameAttempt existing = GameAttempt.builder().userId(user.getId())
                .count(attemptCount).adCount(adCount).expiration(100).build();

        // when
        when(gameAttemptRepository.findById(user.getId())).thenReturn(Optional.of(existing));
        GameAttemptResponse resp = gamePointService.getGameAttemptCount(email);

        // then
        assertThat(resp.count()).isEqualTo(attemptCount);
        assertThat(resp.adCount()).isEqualTo(adCount);
        verify(gameAttemptRepository, never()).save(any());
    }

}