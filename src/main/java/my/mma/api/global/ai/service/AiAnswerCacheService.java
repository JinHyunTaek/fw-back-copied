package my.mma.api.global.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static my.mma.api.global.redis.prefix.RedisKeyPrefix.AI_ANSWER_PREFIX;

/**
 * LLM 응답(질문-답변)을 Redis에 key-value로 캐싱한다.
 * 키는 호출 측이 넘기는 cacheKey(faqId, fight:{eventId}:{fightId} 등)에 prefix만 붙인다.
 * - 예측 답변은 cacheKey에 eventId를 포함시켜, 주간 이벤트가 바뀌면 자연히 새 키가 되도록 한다.
 * - @Cacheable 은 반환값(완성된 텍스트)만 다룰 수 있어 실시간 스트리밍과 양립하지 않으므로
 *   스트림 누적분을 직접 저장하는 수동 캐싱을 사용한다. (TTL 도 여기서 관리)
 */
@Service
@RequiredArgsConstructor
public class AiAnswerCacheService {

    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public String get(String cacheKey) {
        return stringRedisTemplate.opsForValue().get(key(cacheKey));
    }

    public void save(String cacheKey, String answer) {
        if (answer == null || answer.isBlank()) return;
        stringRedisTemplate.opsForValue().set(key(cacheKey), answer, TTL);
    }

    private String key(String cacheKey) {
        return AI_ANSWER_PREFIX.getPrefix() + cacheKey;
    }

}
