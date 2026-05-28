package my.mma.api.global.redis.key;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis 단일 고정 키 관리 enum
 * prefix:{id} 형태의 동적 키는 RedisKeyPrefix 사용
 */
@Getter
@RequiredArgsConstructor
public enum RedisKey {

    CURRENT_EVENT("current-event");

    private final String key;
}
