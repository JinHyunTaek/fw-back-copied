package my.mma.api.game.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.game.service.GameCacheService;
import my.mma.api.global.logaop.Loggable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 게임 출제 데이터 캐시를 매일 0시에 갱신한다.
 * 비우지 않고 새 값으로 덮어쓰기 때문에(GameCacheService 의 @CachePut) 사용자는 캐시 미스를 만나지 않는다.
 * 갱신이 실패하면 기존 값이 그대로 유지되므로, DB 장애 시에도 게임은 계속 제공된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameCacheRefreshScheduler {

    private final GameCacheService gameCacheService;

    @Loggable
    @Scheduled(cron = "0 0 0 * * *")
    public void refreshGameCaches() {
        refresh("popularFighterNamePairs", gameCacheService::refreshPopularFighterNamePairs);
        refresh("allFighterNamePairs", gameCacheService::refreshAllFighterNamePairs);
        refresh("todayFightGamesNormal", gameCacheService::refreshTodayFightGamesNormal);
        refresh("todayFightGamesHard", gameCacheService::refreshTodayFightGamesHard);
    }

    // 하나가 실패해도 나머지 캐시는 갱신한다.
    private void refresh(String cacheName, Supplier<?> loader) {
        try {
            loader.get();
        } catch (Exception e) {
            log.error("[GAME CACHE] 갱신 실패 — 기존 값 유지. cache={}", cacheName, e);
        }
    }

}
