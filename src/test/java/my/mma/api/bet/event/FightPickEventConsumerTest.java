package my.mma.api.bet.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static my.mma.api.bet.event.FightPickEvent.PickSide.FIRST;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 멱등성 마커의 상태 전이를 검증한다.
 * Redis 를 목으로 두되 SETNX/DEL 의 실제 동작(최초 1회만 true)을 흉내 내야
 * "실패 후 재시도" 경로가 재현된다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FightPickEventConsumer — 멱등성 마커")
class FightPickEventConsumerTest {

    @InjectMocks
    private FightPickEventConsumer consumer;

    @Mock
    private FightPickCountApplier applier;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    /** 실제 Redis 처럼 동작하는 키 저장소 */
    private final Set<String> redis = new HashSet<>();

    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        // SETNX: 없을 때만 저장하고 true
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willAnswer(inv -> redis.add(inv.getArgument(0)));
    }

    @Test
    @DisplayName("정상 처리되면 두 번째 수신부터는 집계로 넘기지 않는다")
    void skipsRedeliveryAfterSuccess() {
        FightPickEvent event = FightPickEvent.of(1L, FIRST, 1);

        consumer.consume(List.of(event));
        consumer.consume(List.of(event));   // 재수신

        verify(applier, times(1)).apply(any());
    }

    @Test
    @DisplayName("집계가 실패하면 마커를 되돌려, 재시도 때 다시 집계를 시도한다")
    void rollsBackMarkerWhenApplyFails() {
        // DEL: 저장소에서 제거
        given(stringRedisTemplate.delete(anyString()))
                .willAnswer(inv -> redis.remove(inv.getArgument(0)));
        willThrow(new RuntimeException("집계 실패")).given(applier).apply(any());

        FightPickEvent event = FightPickEvent.of(1L, FIRST, 1);

        // 1회차 — 예외가 밖으로 전파돼야 컨테이너가 재시도한다
        assertThatThrownBy(() -> consumer.consume(List.of(event)))
                .isInstanceOf(RuntimeException.class);

        // 2회차(재시도) — 마커가 롤백됐으므로 걸러지지 않고 다시 시도된다
        assertThatThrownBy(() -> consumer.consume(List.of(event)))
                .isInstanceOf(RuntimeException.class);

        verify(applier, times(2)).apply(any());
    }
}
