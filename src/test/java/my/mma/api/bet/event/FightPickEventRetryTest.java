package my.mma.api.bet.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * "처리 실패 → 오프셋 커밋 안 됨 → 같은 레코드 재수신" 이 실제로 일어나는지 확인한다.
 * 재수신은 우리 코드가 아니라 DefaultErrorHandler 의 seek + 컨테이너 재polling 이 만들어낸다.
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
                "spring.kafka.consumer.group-id=fight-pick-retry-test",
                "spring.kafka.listener.type=batch"
        })
@EmbeddedKafka(partitions = 3, topics = {"fight-pick", "fight-pick.DLT"})
@DirtiesContext
@DisplayName("처리 실패 시 재수신")
class FightPickEventRetryTest {

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
    @DisplayName("집계가 실패하면 오프셋이 커밋되지 않아 같은 메시지를 다시 받는다 (FixedBackOff 2회 → 총 3번)")
    void redeliversWhenProcessingFails() {
        // 매번 처음 보는 메시지로 취급 → 멱등성 필터를 통과시켜 재수신 자체를 관찰한다
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);
        willThrow(new RuntimeException("집계 실패")).given(applier).apply(any());

        publisher.publish(List.of(FightPickEvent.of(1L, FIRST, 1)));

        // 최초 1회 + 재시도 2회. 우리 코드에는 재시도 로직이 없다 — 컨테이너가 되감아 다시 준 것이다.
        verify(applier, timeout(30_000).times(3)).apply(any());
    }
}
