package my.mma.api.global.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import my.mma.api.fighter.dto.RankersPerCategory;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.redis.utils.RedisListUtils;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.ssl.enabled}")
    private boolean useSsl;

    /**
     * Redis 연결을 위한 'Connection' 생성.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig =
                new RedisStandaloneConfiguration(host, port);

        LettuceClientConfiguration.LettuceClientConfigurationBuilder configBuilder =
                LettuceClientConfiguration.builder();
        if(useSsl)
            configBuilder.useSsl();
        return new LettuceConnectionFactory(redisConfig, configBuilder.build());
    }

    /**
     * Redis 데이터 처리를 위한 템플릿을 구성
     * 구성된 RedisTemplate 을 통해서 데이터 통신으로 처리되는 대한 직렬화 수행
     * @return RedisTemplate<String, Object>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // Redis 연결
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        // Key-Value 형태로 직렬화 수행
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        // Hash Key-Value 형태로 직렬화 수행
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        // 기본적 직렬화 수행
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public RedisUtils<CurrentEventDto> streamFightEventRedisUtils(
            @Qualifier("streamFightEventRedisTemplate") RedisTemplate<String, CurrentEventDto> redisTemplate) {
        return new RedisUtils<>(redisTemplate);
    }

    @Bean
    public RedisTemplate<String, CurrentEventDto> streamFightEventRedisTemplate()
    {
        return setRedisTemplate(CurrentEventDto.class);
    }

    @Bean
    public RedisUtils<RankersPerCategory> rankerRedisUtils(
            @Qualifier("rankersRedisTemplate") RedisTemplate<String, RankersPerCategory> redisTemplate) {
        return new RedisUtils<>(redisTemplate);
    }

    @Bean
    public RedisTemplate<String, RankersPerCategory> rankersRedisTemplate()
    {
        return setRedisTemplate(RankersPerCategory.class);
    }

    @Bean
    public RedisListUtils stringRedisUtils(StringRedisTemplate stringRedisTemplate) {
        return new RedisListUtils(stringRedisTemplate);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    private <T> RedisTemplate<String, T> setRedisTemplate(Class<T> clazz){
        RedisTemplate<String, T> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // key는 문자열
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 등 Java 8 날짜 객체 직렬화 지원
        Jackson2JsonRedisSerializer<T> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,clazz);
        redisTemplate.setValueSerializer(serializer); // value는 json type
        return redisTemplate;
    }

}
