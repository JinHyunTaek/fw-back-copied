package my.mma.api.bet.event;

import lombok.RequiredArgsConstructor;
import my.mma.api.bet.repository.FightPickCountRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Comparator.comparingLong;
import static my.mma.api.bet.event.FightPickEvent.PickSide.FIRST;

/**
 * 픽 이벤트를 DB 카운트에 반영한다. direct 구현과 카프카 컨슈머가 공유하는 유일한 집계 경로다.
 * <p>
 * (fightId, side) 로 묶어 delta 를 합산한 뒤 그룹당 UPDATE 1회만 실행한다.
 * 요청 단위(카드 1~3장)에서는 기존과 쿼리 수가 같지만, 컨슈머가 배치로 넘길 때는
 * 이벤트 N건이 그룹 수만큼의 UPDATE 로 줄어든다.
 */
@Component
@RequiredArgsConstructor
public class FightPickCountApplier {

    private final FightPickCountRepository fightPickCountRepository;

    @Transactional
    public void apply(List<FightPickEvent> events) {
        // fightId 오름차순으로 잠근다. 픽 카운트 row 를 만지는 경로가 여기 하나로 모이므로
        // 예측·취소 어느 쪽이든 락 획득 순서가 같아져 데드락이 성립하지 않는다.
        Map<Key, Integer> merged = new TreeMap<>(
                comparingLong(Key::fightId).thenComparing(Key::side));
        for (FightPickEvent event : events) {
            merged.merge(new Key(event.fightId(), event.side()), event.delta(), Integer::sum);
        }
        merged.forEach((key, delta) -> {
            if (delta == 0) return;
            if (key.side() == FIRST) {
                fightPickCountRepository.updateFirstFighterPick(key.fightId(), delta);
            } else {
                fightPickCountRepository.updateLastFighterPick(key.fightId(), delta);
            }
        });
    }

    private record Key(long fightId, FightPickEvent.PickSide side) {}
}
