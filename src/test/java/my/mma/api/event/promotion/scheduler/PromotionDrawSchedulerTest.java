package my.mma.api.event.promotion.scheduler;

import jakarta.persistence.EntityManager;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.GifticonCategory;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.repository.GifticonRepository;
import my.mma.api.event.promotion.repository.PromotionRepository;
import my.mma.api.event.promotion.repository.PromotionWinnerRepository;
import my.mma.api.event.promotion.service.PromotionDrawService;
import my.mma.api.global.config.JpaAuditingConfig;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 추첨 스케줄러 경로 검증 — 자정 대신 drawDuePromotions()를 직접 호출해
 * "발표일이 도래한 프로모션만 추첨된다"를 확인한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PromotionDrawScheduler.class, PromotionDrawService.class, JpaAuditingConfig.class})
class PromotionDrawSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired UserRepository userRepository;
    @Autowired BetRepository betRepository;
    @Autowired BetCardRepository betCardRepository;
    @Autowired PromotionRepository promotionRepository;
    @Autowired GifticonRepository gifticonRepository;
    @Autowired PromotionWinnerRepository promotionWinnerRepository;
    @Autowired PromotionDrawScheduler scheduler;
    @Autowired EntityManager em;

    @MockBean S3ImgService s3ImgService;

    private Promotion savePromotion(LocalDate announceDate) {
        return promotionRepository.save(Promotion.builder()
                .title("테스트 프로모션")
                .startDate(LocalDate.now(KST).minusDays(3))
                .endDate(LocalDate.now(KST).plusDays(3))
                .announceDate(announceDate)
                .maxWinnerCount(2)
                .build());
    }

    private void saveGifticons(Promotion promotion, int n) {
        for (int i = 1; i <= n; i++) {
            gifticonRepository.save(Gifticon.builder()
                    .name("상품" + i).couponNumber("C" + i).imageKey("key" + i)
                    .category(GifticonCategory.COFFEE).displayOrder(i)
                    .isAssigned(false).promotion(promotion).build());
        }
    }

    private void savePredictions(int userIndex, int count) {
        User user = userRepository.save(User.builder()
                .email("u" + userIndex + "@test.com").nickname("user" + userIndex)
                .role("ROLE_USER").password("pwd").point(0).build());
        Bet bet = betRepository.save(Bet.builder().user(user).seedPoint(100).build());
        for (int i = 0; i < count; i++) {
            betCardRepository.save(BetCard.builder().bet(bet).build());
        }
    }

    @Test
    @DisplayName("발표일이 오늘인 프로모션은 스케줄러가 추첨한다")
    void drawsPromotionWhoseAnnounceDateIsToday() {
        // given
        Promotion promotion = savePromotion(LocalDate.now(KST)); // 발표일 = 오늘
        saveGifticons(promotion, 2);
        savePredictions(1, 3);
        savePredictions(2, 2);
        em.flush(); em.clear();

        // when
        scheduler.drawDuePromotions();
        em.flush(); em.clear();

        // then
        assertThat(promotionRepository.findById(promotion.getId()).orElseThrow().isDrawn()).isTrue();
        assertThat(promotionWinnerRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("발표일이 아직 안 된(내일) 프로모션은 추첨되지 않는다")
    void skipsPromotionWhoseAnnounceDateIsFuture() {
        // given
        Promotion promotion = savePromotion(LocalDate.now(KST).plusDays(1)); // 발표일 = 내일
        saveGifticons(promotion, 2);
        savePredictions(1, 2);
        em.flush(); em.clear();

        // when
        scheduler.drawDuePromotions();
        em.flush(); em.clear();

        // then
        assertThat(promotionRepository.findById(promotion.getId()).orElseThrow().isDrawn()).isFalse();
        assertThat(promotionWinnerRepository.count()).isZero();
    }
}
