package my.mma.api.bet.event;

import java.util.List;

/**
 * 픽 카운트 증감을 어떻게 전달할지에 대한 이음매.
 * 운영은 트랜잭션 안에서 즉시 반영(direct), 개발은 카프카 발행(kafka) 구현을 쓴다.
 * 실제 집계 로직은 어느 쪽이든 {@link FightPickCountApplier} 한 벌만 사용한다.
 */
public interface FightPickEventPublisher {

    void publish(List<FightPickEvent> events);
}
