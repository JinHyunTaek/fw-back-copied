package my.mma.api.event.promotion.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.repository.PromotionRepository;
import my.mma.api.event.promotion.service.PromotionDrawService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * announceDate(당첨자 발표일) 기준 추첨 스케줄러.
 *
 * 매일 자정(KST)에 "발표일이 도래했고 아직 추첨되지 않은" 프로모션을 훑어 추첨한다.
 * - announceDate가 날짜(day) 단위이므로 하루 한 번이면 충분(초 단위 정밀 예약 불필요).
 * - 조건이 announceDate <= 오늘 이라, 자정에 서버가 죽어도 다음 실행이 밀린 건을 처리(복구).
 * - 실제 추첨 로직은 {@link PromotionDrawService#draw(Long)} 에 위임(사용자 구현).
 *
 * 멱등성: draw() 내부에서 재조회 후 isDrawn()이면 skip + PromotionWinner 유니크 제약으로 이중 방어.
 * 다중 인스턴스: 아래 @SchedulerLock 활성화(ShedLock LockProvider 빈 설정 필요).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionDrawScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PromotionRepository promotionRepository;
    private final PromotionDrawService promotionDrawService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void drawDuePromotions() {
        LocalDate today = LocalDate.now(KST);
        List<Promotion> targets =
                promotionRepository.findByAnnounceDateAndDrawnAtIsNull(today);

        int success = 0;
        for (Promotion promotion : targets) {
            try {
                promotionDrawService.draw(promotion.getId()); // 각 건은 draw() 내부에서 별도 트랜잭션(재조회)
                success++;
            } catch (Exception e) {
                // 한 건 실패해도 나머지는 계속 (예외 전파 방지)
                log.error("[추첨] 실패: promotionId={}", promotion.getId(), e);
            }
        }
        log.info("[추첨] 완료: 대상 {}건, 성공 {}건", targets.size(), success);
    }

}
