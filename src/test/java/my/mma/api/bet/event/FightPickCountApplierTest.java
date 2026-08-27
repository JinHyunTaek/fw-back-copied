package my.mma.api.bet.event;

import my.mma.api.bet.repository.FightPickCountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static my.mma.api.bet.event.FightPickEvent.PickSide.FIRST;
import static my.mma.api.bet.event.FightPickEvent.PickSide.LAST;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("FightPickCountApplier — 픽 이벤트 집계")
class FightPickCountApplierTest {

    @InjectMocks
    private FightPickCountApplier applier;

    @Mock
    private FightPickCountRepository fightPickCountRepository;

    @Test
    @DisplayName("같은 경기·같은 쪽 이벤트는 합산해 UPDATE 를 1회만 실행한다")
    void mergesSameFightAndSide() {
        applier.apply(List.of(
                FightPickEvent.of(1L, FIRST, 1),
                FightPickEvent.of(1L, FIRST, 1),
                FightPickEvent.of(1L, FIRST, 1)
        ));

        verify(fightPickCountRepository).updateFirstFighterPick(1L, 3);
        verifyNoMoreInteractions(fightPickCountRepository);
    }

    @Test
    @DisplayName("증감이 상쇄되면 쿼리를 실행하지 않는다")
    void skipsWhenDeltaIsZero() {
        applier.apply(List.of(
                FightPickEvent.of(1L, FIRST, 1),
                FightPickEvent.of(1L, FIRST, -1)
        ));

        verifyNoMoreInteractions(fightPickCountRepository);
    }

    @Test
    @DisplayName("같은 경기라도 선수 쪽이 다르면 각각 반영한다")
    void separatesSides() {
        applier.apply(List.of(
                FightPickEvent.of(1L, FIRST, 1),
                FightPickEvent.of(1L, LAST, 2)
        ));

        verify(fightPickCountRepository).updateFirstFighterPick(1L, 1);
        verify(fightPickCountRepository).updateLastFighterPick(1L, 2);
    }

    @Test
    @DisplayName("이벤트 순서와 무관하게 항상 fightId 오름차순으로 잠근다 (데드락 방지)")
    void alwaysLocksInAscendingFightIdOrder() {
        applier.apply(List.of(
                FightPickEvent.of(3L, FIRST, 1),
                FightPickEvent.of(1L, FIRST, 1),
                FightPickEvent.of(2L, FIRST, 1)
        ));

        InOrder inOrder = inOrder(fightPickCountRepository);
        inOrder.verify(fightPickCountRepository).updateFirstFighterPick(1L, 1);
        inOrder.verify(fightPickCountRepository).updateFirstFighterPick(2L, 1);
        inOrder.verify(fightPickCountRepository).updateFirstFighterPick(3L, 1);
    }

    @Test
    @DisplayName("빈 이벤트 목록이면 아무것도 하지 않는다")
    void doesNothingWhenEmpty() {
        applier.apply(List.of());

        verifyNoMoreInteractions(fightPickCountRepository);
    }
}
