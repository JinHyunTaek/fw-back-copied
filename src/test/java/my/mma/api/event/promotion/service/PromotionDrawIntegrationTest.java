package my.mma.api.event.promotion.service;

import jakarta.persistence.EntityManager;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.event.promotion.dto.PromotionDetailDto.PromotionWinnerGifticonDto;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.GifticonCategory;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.entity.PromotionWinner;
import my.mma.api.event.promotion.repository.GifticonRepository;
import my.mma.api.event.promotion.repository.PromotionRepository;
import my.mma.api.event.promotion.repository.PromotionWinnerRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 추첨 통합 테스트 (실 DB) — 스케줄러/모바일 없이 draw()를 직접 트리거해 DB 상태를 검증한다.
 * User → Bet → BetCard(기간 내 createdDateTime) → 프로모션/기프티콘을 실제로 persist.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // application-test.yml(H2 MODE=MySQL) 사용
@Import({PromotionDrawService.class, JpaAuditingConfig.class})               // 서비스 + createdDateTime 감사 활성화
class PromotionDrawIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired BetRepository betRepository;
    @Autowired BetCardRepository betCardRepository;
    @Autowired PromotionRepository promotionRepository;
    @Autowired GifticonRepository gifticonRepository;
    @Autowired PromotionWinnerRepository promotionWinnerRepository;
    @Autowired PromotionDrawService drawService;
    @Autowired EntityManager em;

    @MockBean S3ImgService s3ImgService; // 잉여 삭제 시 afterCommit S3 호출 대체(테스트는 롤백이라 미실행)

    // ===== seed helpers =====
    private User saveUser(int i) {
        return userRepository.save(User.builder()
                .email("u" + i + "@test.com").nickname("user" + i)
                .role("ROLE_USER").password("pwd").point(0).build());
    }

    /** 해당 유저가 count번 예측(BetCard)한 것으로 seed. createdDateTime은 auditing이 now로 세팅 → 프로모션 기간 안에 들어옴 */
    private void savePredictions(User user, int count) {
        Bet bet = betRepository.save(Bet.builder().user(user).seedPoint(100).build());
        for (int i = 0; i < count; i++) {
            betCardRepository.save(BetCard.builder().bet(bet).build());
        }
    }

    private Promotion savePromotion(int maxWinnerCount) {
        return promotionRepository.save(Promotion.builder()
                .title("테스트 프로모션")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(3))     // 기간이 오늘 포함
                .announceDate(LocalDate.now().plusDays(4))
                .maxWinnerCount(maxWinnerCount)
                .build());
    }

    private void saveGifticons(Promotion promotion, int n) {
        for (int i = 1; i <= n; i++) {
            gifticonRepository.save(Gifticon.builder()
                    .name("상품" + i).couponNumber("C" + i).imageKey("key" + i)
                    .displayOrder(i).isAssigned(false).promotion(promotion).build());
        }
    }

    // ===================================================================

    @Test
    @DisplayName("참가자 2명 < 상품 3개: 2명 당첨(중복 없음) + 남은 기프티콘 1개 삭제 + markDrawn")
    void draw_fewerParticipantsThanPrizes() {
        // given
        Promotion promotion = savePromotion(3);
        saveGifticons(promotion, 3);
        savePredictions(saveUser(1), 3);
        savePredictions(saveUser(2), 2);
        em.flush(); em.clear();

        // when
        drawService.draw(promotion.getId());
        em.flush(); em.clear();

        // then
        List<PromotionWinner> winners = promotionWinnerRepository.findAll();
        assertThat(winners).hasSize(2);                                        // 참가자 수만큼만
        assertThat(winners.stream().map(w -> w.getWinner().getId()).distinct()).hasSize(2); // 중복 없음

        List<Gifticon> remaining = gifticonRepository.findAll();
        assertThat(remaining).hasSize(2);                                      // 잉여 1개 삭제됨
        assertThat(remaining).allMatch(Gifticon::isAssigned);                  // 남은 건 모두 배정

        assertThat(promotionRepository.findById(promotion.getId()).orElseThrow().isDrawn()).isTrue();
    }

    @Test
    @DisplayName("참가자 5명 >= 상품 3개: 3명 당첨(중복 없음), 잉여 없음")
    void draw_moreParticipantsThanPrizes() {
        // given
        Promotion promotion = savePromotion(3);
        saveGifticons(promotion, 3);
        for (int i = 1; i <= 5; i++) savePredictions(saveUser(i), i); // 예측 수 다양
        em.flush(); em.clear();

        // when
        drawService.draw(promotion.getId());
        em.flush(); em.clear();

        // then
        List<PromotionWinner> winners = promotionWinnerRepository.findAll();
        assertThat(winners).hasSize(3);                                        // 상품 수만큼
        assertThat(winners.stream().map(w -> w.getWinner().getId()).distinct()).hasSize(3);
        assertThat(gifticonRepository.findAll()).hasSize(3).allMatch(Gifticon::isAssigned);
    }

    @Test
    @DisplayName("당첨자 조회(DTO)는 기프티콘 우선순위(displayOrder) 오름차순 + 닉네임·카테고리를 함께 담는다")
    void winnerDto_orderedByPriority_withNicknameAndCategory() {
        // given: 우선순위(displayOrder)와 카테고리가 서로 다른 기프티콘 3개, 참가자 3명(전원 당첨)
        Promotion promotion = savePromotion(3);
        gifticonRepository.save(Gifticon.builder().name("스타벅스").couponNumber("A").imageKey("a")
                .category(GifticonCategory.COFFEE).displayOrder(1).isAssigned(false).promotion(promotion).build());
        gifticonRepository.save(Gifticon.builder().name("BBQ").couponNumber("B").imageKey("b")
                .category(GifticonCategory.CHICKEN).displayOrder(2).isAssigned(false).promotion(promotion).build());
        gifticonRepository.save(Gifticon.builder().name("배민상품권").couponNumber("C").imageKey("c")
                .category(GifticonCategory.DELIVERY).displayOrder(3).isAssigned(false).promotion(promotion).build());
        for (int i = 1; i <= 3; i++) savePredictions(saveUser(i), 2);
        em.flush(); em.clear();

        drawService.draw(promotion.getId());
        em.flush(); em.clear();

        // when: 상세 화면이 쓰는 fetch join 쿼리 → DTO 매핑
        List<PromotionWinnerGifticonDto> winners =
                promotionWinnerRepository.findWinnerGifticonInfoByPromotionId(promotion.getId())
                        .stream().map(PromotionWinnerGifticonDto::of).toList();

        // then: displayOrder 오름차순으로 정렬 + 카테고리 포함 + 닉네임(fetch join) 로드
        assertThat(winners).hasSize(3);
        assertThat(winners).extracting(PromotionWinnerGifticonDto::gifticonName)
                .containsExactly("스타벅스", "BBQ", "배민상품권");
        assertThat(winners).extracting(PromotionWinnerGifticonDto::category)
                .containsExactly(GifticonCategory.COFFEE, GifticonCategory.CHICKEN, GifticonCategory.DELIVERY);
        assertThat(winners).allSatisfy(w -> assertThat(w.winnerNickname()).isNotBlank());
    }

    @Test
    @DisplayName("이미 추첨된 프로모션은 재실행해도 당첨자가 늘지 않는다 (멱등)")
    void draw_idempotent() {
        // given
        Promotion promotion = savePromotion(3);
        saveGifticons(promotion, 3);
        for (int i = 1; i <= 3; i++) savePredictions(saveUser(i), 2);
        em.flush(); em.clear();

        drawService.draw(promotion.getId());
        em.flush(); em.clear();
        long afterFirst = promotionWinnerRepository.count();

        // when: 다시 호출
        drawService.draw(promotion.getId());
        em.flush(); em.clear();

        // then
        assertThat(promotionWinnerRepository.count()).isEqualTo(afterFirst);
    }
}
