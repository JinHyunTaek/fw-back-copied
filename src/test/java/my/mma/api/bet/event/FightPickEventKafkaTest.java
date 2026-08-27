package my.mma.api.bet.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;

import static my.mma.api.bet.event.FightPickEvent.PickSide.FIRST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * 발행 → 브로커 → 배치 컨슈머 → 집계까지의 배선을 검증한다.
 * 도커 없이 임베디드 브로커로 돌아가므로 CI 에서도 그대로 실행된다.
 */
@SpringBootTest(
        classes = {
                KafkaAutoConfiguration.class,
                my.mma.api.global.kafka.KafkaConfig.class,
                KafkaFightPickEventPublisher.class,
                FightPickEventConsumer.class
        },
        properties = {
                "messaging.mode=kafka",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
                "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
                "spring.kafka.consumer.properties.spring.json.trusted.packages=my.mma.api.bet.event",
                "spring.kafka.consumer.properties.spring.json.value.default.type=my.mma.api.bet.event.FightPickEvent",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.group-id=fight-pick-aggregator-test",
                "spring.kafka.listener.type=batch"
        })
@EmbeddedKafka(partitions = 3, topics = {"fight-pick", "fight-pick.DLT"})
@DirtiesContext
@DisplayName("픽 이벤트 카프카 파이프라인")
class FightPickEventKafkaTest {

    @Autowired
    private FightPickEventPublisher publisher;

    @MockBean
    private FightPickCountApplier applier;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("발행한 이벤트가 컨슈머까지 도달해 집계로 넘어간다")
    void publishedEventsReachTheAggregator() {
        // 처음 보는 eventId → 전부 처리 대상
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        publisher.publish(List.of(
                FightPickEvent.of(1L, FIRST, 1),
                FightPickEvent.of(1L, FIRST, 1),
                FightPickEvent.of(2L, FIRST, 1)
        ));

        ArgumentCaptor<List<FightPickEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(applier, timeout(20_000).atLeastOnce()).apply(captor.capture());

        List<FightPickEvent> received = captor.getAllValues().stream().flatMap(List::stream).toList();
        assertThat(received).hasSize(3);
        assertThat(received).extracting(FightPickEvent::fightId).containsExactlyInAnyOrder(1L, 1L, 2L);
    }

    @Test
    @DisplayName("이미 처리한 이벤트는 집계로 넘기지 않는다 (멱등성)")
    void skipsAlreadyProcessedEvents() {
        // SETNX 가 false → 재처리로 판정
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(false);

        publisher.publish(List.of(FightPickEvent.of(3L, FIRST, 1)));

        // 컨슈머가 실제로 수신했지만 걸러냈음을 보이기 위해, 충분히 기다린 뒤 호출이 없었음을 확인한다
        verify(applier, after(8_000).never()).apply(any());
    }
}
