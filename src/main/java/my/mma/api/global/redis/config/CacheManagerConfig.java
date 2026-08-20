package my.mma.api.global.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import my.mma.api.global.resolver.RecordSupportingTypeResolver;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheManagerConfig {

    private static final Duration GAME_CACHE_TTL = Duration.ofHours(26);

    /**
     * JVM 캐시(ConcurrentMapCacheManager) 쓰는 경우,
     * 서버 2대 이상일 경우 캐시 데이터 동기화되지 않을 수 있음
     * RedisCacheManager 사용할 경우, 단일 레디스 저장소에 데이터를 캐싱하여
     * 데이터 불일치 문제가 없다.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory){
        ObjectMapper objectMapper = new ObjectMapper();
        RecordSupportingTypeResolver typeResolver = new RecordSupportingTypeResolver(
                ObjectMapper.DefaultTyping.NON_FINAL, objectMapper.getPolymorphicTypeValidator());
        StdTypeResolverBuilder initializedResolver = typeResolver.init(JsonTypeInfo.Id.CLASS, null);
        initializedResolver = initializedResolver.inclusion(JsonTypeInfo.As.PROPERTY);
        objectMapper.setDefaultTyping(initializedResolver);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );

        // 게임 캐시는 매일 0시 스케줄러가 덮어쓰는 것을 정상 경로로 삼는다.
        // TTL 이 갱신 주기(24h)와 같으면 만료가 cron 보다 먼저 끊기는 순간 캐시 미스가 생기므로
        // 갱신 주기보다 길게 잡아 TTL 은 스케줄러가 멈췄을 때의 안전망으로만 동작하게 한다.
        RedisCacheConfiguration gameCacheConfig = config.entryTtl(GAME_CACHE_TTL);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("popularFighterNamePairs", gameCacheConfig)
                .withCacheConfiguration("allFighterNamePairs", gameCacheConfig)
                .withCacheConfiguration("todayFightGamesNormal", gameCacheConfig)
                .withCacheConfiguration("todayFightGamesHard", gameCacheConfig)
                .build();
    }

}
