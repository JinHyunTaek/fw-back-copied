package my.mma.api.bet.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static my.mma.api.global.kafka.KafkaTopics.FIGHT_PICK;

/**
 * 개발 환경 구현. 픽 카운트 갱신을 베팅 트랜잭션에서 떼어내 카프카로 넘긴다.
 * <p>
 * 발행 시점은 <b>커밋 이후</b>다. 트랜잭션 안에서 보내면 이후 롤백됐을 때
 * 이미 나간 이벤트를 되돌릴 수 없어 카운트가 틀어진다(dual write).
 * 다만 커밋 직후 브로커 전송이 실패하면 그 증감은 유실되므로,
 * 완전한 보장이 필요하면 outbox 테이블을 경유해야 한다. 여기서는 채택하지 않았다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.mode", havingValue = "kafka")
public class KafkaFightPickEventPublisher implements FightPickEventPublisher {

    private final KafkaTemplate<String, FightPickEvent> kafkaTemplate;

    @Override
    public void publish(List<FightPickEvent> events) {
        if (events.isEmpty()) return;

        // 테스트 케이스 혹은 트랜잭션이 적용되지 않은 이벤트 고려
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send(events);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            // 롤백되면 수행 안 함
            @Override
            public void afterCommit() {
                send(events);
            }
        });
    }

    private void send(List<FightPickEvent> events) {
        for (FightPickEvent event : events) {
            // 파티션 키 = fightId → 같은 경기 이벤트의 순서 보장
            kafkaTemplate.send(FIGHT_PICK, String.valueOf(event.fightId()), event)
                    .whenComplete((result, e) -> {
                        if (e != null) {
                            log.error("[PICK] 이벤트 발행 실패. fightId={} messageId={}",
                                    event.fightId(), event.messageId(), e);
                        }
                    });
        }
    }
}
