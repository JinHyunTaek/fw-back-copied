package my.mma.api.global.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmMessageService {

    public static final int BATCH_SIZE = 500;

    private final FcmTokenCleanupService fcmTokenCleanupService;

    // sync & http request * n회 => 매우 느림
    // 관리자 알림 전용. 관리자 토큰은 무효라도 지우지 않는다.
    // 지워버리면 이후 "예외 발생" 알림을 받을 방법이 사라져 장애를 모르게 된다.
    public void sendMessage(Message message) {
        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.error("fcm send failed. errorCode={}", e.getMessagingErrorCode(), e);
        }
    }

    /**
     * async & http request (messages size / BATCH_SIZE)회 & 서로 다른 푸시 메시지 전송.
     *
     * @param tokens messages 와 같은 순서·같은 크기의 토큰 목록.
     *               BatchResponse 는 입력 순서를 그대로 보존하므로 인덱스로 짝지을 수 있다.
     */
    public void sendEach(List<Message> messages, List<String> tokens) {
        if (messages.size() != tokens.size()) {
            throw new IllegalArgumentException(
                    "messages 와 tokens 의 크기가 다릅니다. messages=%d, tokens=%d"
                            .formatted(messages.size(), tokens.size()));
        }
        List<String> invalidTokens = new ArrayList<>();
        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, messages.size());
            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages.subList(i, end));
                collectInvalidTokens(response, tokens.subList(i, end), invalidTokens);
            } catch (FirebaseMessagingException e) {
                log.error("fcm sendEach failed. offset={}, size={}, errorCode={}",
                        i, end - i, e.getMessagingErrorCode(), e);
            }
        }
        cleanUp(invalidTokens);
    }

    /**
     * async & http request & 같은 푸시 메시지만 전송.
     *
     * @param tokens message 를 만들 때 addAllTokens 로 넣은 것과 같은 순서의 토큰 목록
     */
    public void sendEachForMulticast(MulticastMessage message, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            collectInvalidTokens(response, tokens, invalidTokens);
        } catch (FirebaseMessagingException e) {
            log.error("fcm multicast failed. size={}, errorCode={}",
                    tokens.size(), e.getMessagingErrorCode(), e);
        }
        cleanUp(invalidTokens);
    }

    private void collectInvalidTokens(BatchResponse response, List<String> tokens, List<String> invalidTokens) {
        if (response.getFailureCount() == 0) {
            return;
        }
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse each = responses.get(i);
            if (each.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException e = each.getException();
            MessagingErrorCode errorCode = (e == null) ? null : e.getMessagingErrorCode();
            String token = tokens.get(i);
            if (isPermanentlyInvalid(errorCode)) {
                invalidTokens.add(token);
                log.info("fcm token invalidated. errorCode={}, token={}", errorCode, mask(token));
            } else {
                // UNAVAILABLE·INTERNAL 등 일시적 실패. 토큰은 살려두고 다음 발송 때 재시도된다.
                log.warn("fcm send failed (retryable). errorCode={}, token={}, message={}",
                        errorCode, mask(token), (e == null) ? null : e.getMessage());
            }
        }
    }

    /**
     * 재시도해도 절대 성공하지 않는 실패인지 판별한다.
     * UNREGISTERED 는 앱 삭제·재설치·장기 미사용으로 토큰이 폐기된 경우이고, (UNAVAILABLE·INTERNAL은 일시적 장애)
     * 그대로 두면 발송할 때마다 영원히 같은 실패가 반복된다.
     */
    private boolean isPermanentlyInvalid(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED
                || errorCode == MessagingErrorCode.INVALID_ARGUMENT
                || errorCode == MessagingErrorCode.SENDER_ID_MISMATCH;
    }

    /** 발송(외부 HTTP)이 모두 끝난 뒤에만 트랜잭션을 연다. IN 절이 과도해지지 않게 나눠 실행한다. */
    private void cleanUp(List<String> invalidTokens) {
        if (invalidTokens.isEmpty()) {
            return;
        }
        int cleared = 0;
        for (int i = 0; i < invalidTokens.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, invalidTokens.size());
            cleared += fcmTokenCleanupService.clearInvalidTokens(invalidTokens.subList(i, end));
        }
        log.info("cleared invalid fcm tokens. detected={}, updated={}", invalidTokens.size(), cleared);
    }

    /** 토큰 전체를 로그에 남기면 그 자체로 푸시를 보낼 수 있는 값이 노출된다. */
    private String mask(String token) {
        return (token == null || token.length() <= 12) ? "****" : token.substring(0, 12) + "...";
    }
}
