package my.mma.api.event.promotion.scheduler;

import my.mma.api.global.logaop.Loggable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 프로모션 활성 여부는 날짜(startDate ~ endDate) 단위로만 바뀌므로,
 * 활성 집합이 실제로 달라질 수 있는 시점(자정)에만 캐시를 비운다.
 * (관리자 변경으로 인한 갱신은 AdminPromotionService의 @CacheEvict가 담당)
 */
@Component
public class PromotionCacheEvictScheduler {

    @Loggable
    @CacheEvict(value = {"activePromotions","recentEvents"}, allEntries = true)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void evictActivePromotions() {
    }

}
