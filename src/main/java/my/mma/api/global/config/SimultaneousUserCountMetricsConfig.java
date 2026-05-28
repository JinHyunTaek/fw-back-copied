package my.mma.api.global.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import my.mma.api.stream.handler.GlobalWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimultaneousUserCountMetricsConfig {

    @Bean
    public MeterBinder socketConnectedUserCnt(GlobalWebSocketHandler socketHandler){
        return registry -> Gauge.builder("socket.connected.user.count", socketHandler,
                GlobalWebSocketHandler::getSocketConnectedUserCnt).register(registry);
    }

}
