package my.mma.api.global.redis.prefix;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisKeyPrefix {

    GAME_FIGHTER_NAME_PREFIX("game-fighter-name:"),
    CHAT_LOG_PREFIX("chat:"),
    RANKERS_KEY_PREFIX("rankers:"),
    BET_CANCEL_COUNT_PREFIX("bet-cancel-count:"),
    AI_ANSWER_PREFIX("ai-answer:");

    private final String prefix;

}
