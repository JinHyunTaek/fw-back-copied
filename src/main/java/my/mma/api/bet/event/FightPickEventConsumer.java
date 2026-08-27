package my.mma.api.bet.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static my.mma.api.global.kafka.KafkaTopics.FIGHT_PICK;
import static my.mma.api.global.redis.prefix.RedisKeyPrefix.PROCESSED_MESSAGE_PREFIX;

/**
 * 픽 이벤트를 배치로 받아 한 번에 집계한다.
 * 같은 경기 이벤트는 같은 파티션에 순서대로 들어오므로 락 없이 순차 처리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.mode", havingValue = "kafka")
public class FightPickEventConsumer {

    // 처리 이력 보관 기간. 재처리 창(리밸런싱·재시도)만 덮으면 충분하다.
    private static final Duration PROCESSED_TTL = Duration.ofHours(6);

    private final FightPickCountApplier applier;
    private final StringRedisTemplate stringRedisTemplate;

    @KafkaListener(topics = FIGHT_PICK, groupId = "fight-pick-aggregator")
    public void consume(List<FightPickEvent> events) {
        List<FightPickEvent> unprocessed = new ArrayList<>();
        for (FightPickEvent event : events) {
            if (markProcessed(event)) {
                unprocessed.add(event);
            } else {
                log.debug("[PICK] 중복 이벤트 무시. messageId={}", event.messageId());
            }
        }
        if (unprocessed.isEmpty()) return;

        try {
            applier.apply(unprocessed);
        } catch (Exception e) {
            // 반영에 실패했으므로 '처리함' 표시를 되돌린다.
            // 남겨두면 재시도 때 전부 중복으로 걸러져, DB 반영 없이 오프셋만 커밋되고 유실된다.
            unprocessed.forEach(this::unmark);
            throw e;   // 다시 던져야 컨테이너가 재시도하고, 소진되면 DLT 로 보낸다
        }
        log.info("[PICK] 집계 완료. 수신={} 반영={}", events.size(), unprocessed.size());
    }

    /** at-least-once 이므로 같은 이벤트가 다시 올 수 있다. SETNX 로 최초 1회만 통과시킨다. */
    private boolean markProcessed(FightPickEvent event) {
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(key(event), "1", PROCESSED_TTL);
        return Boolean.TRUE.equals(first);
    }

    /** 삭제가 실패해도 원래 예외를 가리지 않도록 여기서 삼킨다(그 메시지만 유실 위험이 남는다). */
    private void unmark(FightPickEvent event) {
        try {
            stringRedisTemplate.delete(key(event));
        } catch (Exception e) {
            log.error("[PICK] 멱등성 마커 롤백 실패. messageId={}", event.messageId(), e);
        }
    }

    private String key(FightPickEvent event) {
        return PROCESSED_MESSAGE_PREFIX.getPrefix() + event.messageId();
    }
}
