package my.mma.api.bet.event;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 운영 기본 구현. 베팅 트랜잭션 안에서 픽 카운트를 즉시 반영한다.
 * 베팅 저장과 카운트가 한 트랜잭션이라 원자적이지만, 그만큼 공유 row 락을 커밋까지 붙잡는다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.mode", havingValue = "direct", matchIfMissing = true)
public class DirectFightPickEventPublisher implements FightPickEventPublisher {

    private final FightPickCountApplier applier;

    @Override
    public void publish(List<FightPickEvent> events) {
        applier.apply(events);
    }
}
