package my.mma.api.global.redis.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@RequiredArgsConstructor
public class RedisUtils<T> {

    private final RedisTemplate<String, T> redisTemplate;

    public void saveData(String key, T data) {
        redisTemplate.opsForValue().set(key, data);
    }

    public void saveDataWithTTL(String key, T data, Duration ttl) {
        redisTemplate.opsForValue().set(key, data, ttl);
    }

    public void updateData(String key, T data) {
        redisTemplate.opsForValue().set(key, data);
    }

    public T getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }


}
