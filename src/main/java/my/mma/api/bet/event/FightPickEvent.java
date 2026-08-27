package my.mma.api.bet.event;

import java.util.UUID;

/**
 * 경기별 픽 카운트 증감 이벤트.
 * <p>
 * 카프카 토픽에 실리므로 JPA 엔티티가 아닌 순수 값 객체로 둔다.
 * 파티션 키는 fightId 이며, 같은 경기의 이벤트가 한 파티션에 순서대로 쌓여
 * 단일 컨슈머가 순차 집계하므로 DB 공유 row 락이 필요 없어진다.
 *
 * @param messageId 메시지 식별자 겸 멱등성 키. 컨슈머 재처리(at-least-once) 시 중복 집계를 막는다.
 *                  도메인의 eventId(=FightEvent id)와 혼동하지 않도록 이름을 분리
 * @param fightId   대상 경기(FighterFightEvent) id. 파티션 키.
 * @param side      어느 선수 쪽 픽인지
 * @param delta     +1 예측, -1 예측 취소
 */
public record FightPickEvent(String messageId, long fightId, PickSide side, int delta) {

    public static FightPickEvent of(long fightId, PickSide side, int delta) {
        return new FightPickEvent(UUID.randomUUID().toString(), fightId, side, delta);
    }

    public enum PickSide { FIRST, LAST }
}
