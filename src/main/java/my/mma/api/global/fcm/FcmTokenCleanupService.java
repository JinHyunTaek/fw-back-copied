package my.mma.api.global.fcm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * FCM 이 영구 무효로 응답한 토큰을 사용자 정보에서 비운다.
 * <p>
 * FcmMessageService 와 분리된 별도 빈인 이유가 두 가지다.
 * <ul>
 *   <li>같은 클래스 안에서 호출하면 프록시를 거치지 않아 {@code @Transactional} 이 무시된다.</li>
 *   <li>FCM 발송은 외부 HTTP 라 수 초 이상 걸린다. 트랜잭션 안에서 발송하면 그동안 DB 커넥션을
 *       붙들게 되는데, 커넥션 풀이 8개뿐이라 스케줄러가 겹치면 고갈될 수 있다.
 *       그래서 발송이 전부 끝난 뒤 이 메서드만 짧게 트랜잭션을 연다.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FcmTokenCleanupService {

    private final UserRepository userRepository;

    /**
     * ★ 반드시 '토큰 값' 기준으로 지운다. 사용자 id 기준으로 지우면 안 된다.
     * 발송이 진행되는 동안 사용자가 앱을 재설치하고 로그인해 새 토큰이 저장될 수 있는데,
     * id 기준이면 방금 저장된 유효한 토큰까지 날아간다. 값 기준이면 값이 달라 그대로 남는다.
     */
    @Transactional
    public int clearInvalidTokens(Collection<String> invalidTokens) {
        if (invalidTokens == null || invalidTokens.isEmpty()) {
            return 0;
        }
        return userRepository.clearFcmTokens(invalidTokens);
    }
}
