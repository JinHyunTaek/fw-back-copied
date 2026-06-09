package my.mma.api.global.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * 시스템 프롬프트·데이터는 요청 카테고리(fight/fighter)에 따라 AiChatService 가 매번 조립한다.
     * tool calling 대신 필요한 데이터를 직접 프롬프트에 주입하므로 defaultTools/defaultSystem 을 두지 않는다.
     * (자유입력 경로를 다시 열 때를 대비해 FightQueryTools 의 @Tool 정의는 남겨둔다.)
     */
    @Bean
    ChatClient fightweekChatClient(ChatClient.Builder builder){
        return builder.build();
    }

}
