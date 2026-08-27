package my.mma.api.global.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * 개발 환경 전용 카프카 설정. messaging.mode=kafka 일 때만 활성화된다.
 * 운영(messaging.mode=direct)에서는 토픽·리스너·에러 핸들러가 모두 생성되지 않으므로
 * 브로커에 연결을 시도하지 않는다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "messaging.mode", havingValue = "kafka")
public class KafkaConfig {

    // 파티션을 나눠도 키가 fightId 라 같은 경기는 항상 같은 파티션으로 간다(순서 보장).
    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;   // 로컬 단일 브로커
    private static final String DLT_SUFFIX = ".DLT";

    @Bean
    public NewTopic fightPickTopic() {
        return TopicBuilder.name(KafkaTopics.FIGHT_PICK)
                .partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    public NewTopic fightPickDeadLetterTopic() {
        return TopicBuilder.name(KafkaTopics.FIGHT_PICK_DLT)
                .partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    /**
     * (컨슈머가) 메시지 처리(소비) 실패 시 1초 간격 2회 재시도 후 DLT 로 보낸다.
     * 배치 리스너라 재시도·이관 단위는 배치 전체다. 레코드 단위로 끊어내려면
     * 리스너에서 BatchListenerFailedException 에 실패 인덱스를 담아 던져야 한다.
     * <p>
     * 기본 리졸버는 원본과 <b>같은 파티션 번호</b>로 발행해서 DLT 파티션 수가 원본보다 적으면 발행이 실패한다.
     * 파티션을 -1(자동 할당)로 넘겨 그 제약을 없앤다. 키가 fightId 이므로 같은 경기끼리 모이는 성질은 유지된다.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> {
                    log.warn("[KAFKA] 재시도 소진 → DLT 이관. topic={} partition={} offset={} key={}",
                            record.topic(), record.partition(), record.offset(), record.key(), exception);
                    return new TopicPartition(record.topic() + DLT_SUFFIX, -1);
                });
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2));
    }
}
