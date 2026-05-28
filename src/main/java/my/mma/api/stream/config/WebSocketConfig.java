package my.mma.api.stream.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.redis.utils.RedisListUtils;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.stream.handler.GlobalWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final GlobalWebSocketHandler globalWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(globalWebSocketHandler, "/ws/stream")
                .setAllowedOrigins("*");
    }

}
