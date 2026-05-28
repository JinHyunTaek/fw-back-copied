package my.mma.admin.stream.event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

//    @Bean(name = "betExecutor")
//    public Executor betExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(4); // 동시에 처리할 최소 스레드 수
//        executor.setMaxPoolSize(8); // 최대 스레드 수
//        executor.setQueueCapacity(100); // 대기 큐 크기
//        executor.setThreadNamePrefix("Bet-Async-");
//        executor.initialize();
//        return executor;
//    }

//    @Bean(name = "fighter notification")
//    public Executor notificationExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(4); // 동시에 처리할 최소 스레드 수
//        executor.setMaxPoolSize(8); // 최대 스레드 수
//        executor.setQueueCapacity(100); // 대기 큐 크기
//        executor.setThreadNamePrefix("notification");
//        executor.initialize();
//        return executor;
//    }

}
