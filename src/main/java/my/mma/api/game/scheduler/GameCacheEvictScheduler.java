package my.mma.api.game.scheduler;

import my.mma.api.global.logaop.Loggable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GameCacheEvictScheduler {

    @Loggable
    @CacheEvict(value = {
            "popularFighterNamePairs",
            "allFighterNamePairs",
            "todayFightGamesNormal",
            "todayFightGamesHard"
    }, allEntries = true)
    @Scheduled(cron = "0 0 0 * * *")
    public void evictGameCaches() {
    }

}