package my.mma.api.event.promotion.service;

import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.entity.PromotionWinner;
import my.mma.api.event.promotion.repository.GifticonRepository;
import my.mma.api.event.promotion.repository.PromotionRepository;
import my.mma.api.event.promotion.repository.PromotionWinnerRepository;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.user.entity.User;
import my.mma.fixture.entity.user.UserFixture;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionDrawService - 추첨 로직")
class PromotionDrawServiceTest {

    @Mock GifticonRepository gifticonRepository;
    @Mock BetCardRepository betCardRepository;
    @Mock PromotionWinnerRepository promotionWinnerRepository;
    @Mock PromotionRepository promotionRepository;
    @Mock S3ImgService s3ImgService;

    @InjectMocks PromotionDrawService drawService;

    private static final long PROMOTION_ID = 1L;
    private static final int ENTRY_CAP = 5;

    // draw()가 leftover 삭제 시 afterCommit 동기화를 등록하므로, 유닛 테스트에서도 동기화를 활성화해둔다.
    @BeforeEach
    void initTxSync() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTxSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // ===== fixtures =====
    private Promotion promotion(int maxWinnerCount) {
        return Promotion.builder()
                .id(PROMOTION_ID)
                .title("7월 예측 이벤트")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .announceDate(LocalDate.of(2026, 8, 1))
                .maxWinnerCount(maxWinnerCount)
                .build();
    }

    private User user(long id) {
        return UserFixture.builder().id(id).nickname("user" + id).build();
    }

    /** displayOrder 순, imageKey 존재, 미배정 상태의 기프티콘 n개 */
    private List<Gifticon> gifticons(int n, Promotion promotion) {
        List<Gifticon> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(Gifticon.builder()
                    .id((long) i).name("상품" + i).couponNumber("C" + i)
                    .imageKey("key" + i).displayOrder(i).isAssigned(false)
                    .promotion(promotion).build());
        }
        return list;
    }

    /** 특정 유저가 count번 예측한 BetCard 목록 (draw는 bc.getBet().getUser()만 사용) */
    private List<BetCard> betCardsOf(User user, int count) {
        Bet bet = mock(Bet.class);
        lenient().when(bet.getUser()).thenReturn(user);
        List<BetCard> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BetCard card = mock(BetCard.class);
            lenient().when(card.getBet()).thenReturn(bet);
            cards.add(card);
        }
        return cards;
    }

    private void stubBetCards(List<BetCard> cards) {
        when(betCardRepository.findPromotionDurationBetCards(any(), any())).thenReturn(cards);
    }

    // ===================================================================

    @Nested
    @DisplayName("buildTickets() - 응모권 상한")
    class BuildTickets {

        @Test
        @DisplayName("한 유저가 상한보다 많이 예측해도 티켓은 최대 ENTRY_CAP장까지만")
        void cappedAtEntryCap() {
            // given: A 10번(상한 초과), B 3번(상한 이하)
            User a = user(1L), b = user(2L);
            List<BetCard> cards = new ArrayList<>();
            cards.addAll(betCardsOf(a, 10));
            cards.addAll(betCardsOf(b, 3));
            stubBetCards(cards);

            // when
            List<User> tickets = drawService.buildTickets(promotion(3));

            // then
            Map<Long, Long> ticketsByUser = tickets.stream()
                    .collect(Collectors.groupingBy(User::getId,
                            Collectors.counting()));
            assertThat(ticketsByUser.get(1L)).isEqualTo(ENTRY_CAP);
            assertThat(ticketsByUser.get(2L)).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("draw() - 추첨")
    class Draw {

        @Test
        @DisplayName("당첨자는 중복되지 않는다 (한 유저가 여러 상품 못 받음)")
        void winnersAreDistinct() {
            // given: 상품 3개, 참가자 3명(A는 티켓 많음)
            Promotion promotion = promotion(3);
            when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
            when(gifticonRepository.findByPromotionIdOrderByDisplayOrderAsc(PROMOTION_ID))
                    .thenReturn(gifticons(3, promotion));
            List<BetCard> cards = new ArrayList<>();
            cards.addAll(betCardsOf(user(1L), 10));
            cards.addAll(betCardsOf(user(2L), 1));
            cards.addAll(betCardsOf(user(3L), 1));
            stubBetCards(cards);

            // when
            drawService.draw(PROMOTION_ID);

            // then
            ArgumentCaptor<PromotionWinner> captor = ArgumentCaptor.forClass(PromotionWinner.class);
            verify(promotionWinnerRepository, times(3)).save(captor.capture());
            List<Long> winnerIds = captor.getAllValues().stream()
                    .map(w -> w.getWinner().getId()).toList();
            assertThat(winnerIds.size()).isEqualTo(3);
            assertThat(winnerIds).doesNotHaveDuplicates();
            assertThat(promotion.isDrawn()).isTrue();
        }

        @Test
        @DisplayName("참가자 수 < 상품 수면, 참가자 수만큼만 당첨되고 남은 기프티콘은 삭제된다")
        void deletesLeftoverGifticons() {
            // given: 상품 3개, 참가자 2명 → 당첨 2, 잉여 1
            Promotion promotion = promotion(3);
            List<Gifticon> prizes = gifticons(3, promotion);
            when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
            when(gifticonRepository.findByPromotionIdOrderByDisplayOrderAsc(PROMOTION_ID)).thenReturn(prizes);
            List<BetCard> cards = new ArrayList<>();
            cards.addAll(betCardsOf(user(1L), 3));
            cards.addAll(betCardsOf(user(2L), 2));
            stubBetCards(cards);

            // when
            drawService.draw(PROMOTION_ID);

            // then: 당첨자 2명
            verify(promotionWinnerRepository, times(2)).save(any(PromotionWinner.class));
            // 잉여 1개 삭제
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Gifticon>> del = ArgumentCaptor.forClass(List.class);
            verify(gifticonRepository).deleteAll(del.capture());
            assertThat(del.getValue()).hasSize(1);
            // afterCommit 콜백 실행 시 S3 이미지 삭제
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(s3ImgService).deleteGifticonImg(anyString());
        }

        @Test
        @DisplayName("이미 추첨된 프로모션은 아무 작업도 하지 않는다 (멱등)")
        void idempotentWhenAlreadyDrawn() {
            // given
            Promotion promotion = promotion(3);
            promotion.markDrawn();
            when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));

            // when
            drawService.draw(PROMOTION_ID);

            // then
            verify(promotionWinnerRepository, never()).save(any());
            verify(gifticonRepository, never()).findByPromotionIdOrderByDisplayOrderAsc(any());
            verify(gifticonRepository, never()).deleteAll(any());
        }
    }
}
