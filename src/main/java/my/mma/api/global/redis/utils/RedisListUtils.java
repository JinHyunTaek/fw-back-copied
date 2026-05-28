package my.mma.api.global.redis.utils;

import lombok.RequiredArgsConstructor;
import my.mma.api.global.redis.prefix.RedisKeyPrefix;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class RedisListUtils {

    private final StringRedisTemplate redisTemplate;

    public void pushRightWithRange(String key, String message, int range) {
        redisTemplate.opsForList().rightPush(key, message); // 날짜 오름차순 (최신 채팅이 리스트의 끝에 있음)
        redisTemplate.opsForList().trim(key, -range, -1); // 최근 {range}개 유지
        // TTL 없을 때만 설정
        Long ttl = redisTemplate.getExpire(key);
        if (ttl == null || ttl == -1) {
            redisTemplate.expire(key, Duration.ofDays(30));
        }
    }

    public List<String> getRecent(String key, int range) {
        return redisTemplate.opsForList().range(key, 0, range - 1);
    }

}
