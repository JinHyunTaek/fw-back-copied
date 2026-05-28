package my.mma.api.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.game.dto.GameAttemptResponse;
import my.mma.api.game.entity.GameAttempt;
import my.mma.api.game.repository.GameAttemptRepository;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Transactional(readOnly = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class GamePointService {

    private final GameAttemptRepository gameAttemptRepository;
    private final UserRepository userRepository;

    public void updateGameAttemptCount(String email, boolean isSubtract) {
        User user = extractUserByEmail(email);
        GameAttempt gameAttempt = gameAttemptRepository.findById(user.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_EVENT_FOUND_400)
        );
        if (isSubtract) {
            if (gameAttempt.getCount() == 0)
                throw new CustomException(ErrorCode.GAME_ATTEMPT_COUNT_EXPIRED_400);
        }
        // 광고
        else {
            if (gameAttempt.getAdCount() == 0)
                throw new CustomException(ErrorCode.AD_COUNT_EXPIRED_400);
            gameAttempt.setAdCount(gameAttempt.getAdCount() - 1);
        }
        gameAttempt.setCount(gameAttempt.getCount() + (isSubtract ? -1 : 1));
        gameAttemptRepository.save(gameAttempt);
    }

    @Loggable
    @Transactional
    public int updatePoint(String email, int newPoint) {
        User user = extractUserByEmail(email);
        user.updatePoint(newPoint);
        return user.getPoint();
    }

    // gameAttempt 존재
    public GameAttemptResponse getGameAttemptCount(String email) {
        User user = extractUserByEmail(email);
        GameAttempt gameAttempt = gameAttemptRepository.findById(user.getId()).orElseGet(
                () ->
                        gameAttemptRepository.save(GameAttempt.builder()
                                .userId(user.getId())
                                .count(10)
                                .adCount(5)
                                .expiration(Duration.between(
                                        LocalDateTime.now(),
                                        LocalDate.now().plusDays(1).atStartOfDay()).getSeconds()
                                ).build())
        );
        return GameAttemptResponse.builder()
                .count(gameAttempt.getCount())
                .adCount(gameAttempt.getAdCount())
                .build();
    }

    private User extractUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400)
        );
    }

}
