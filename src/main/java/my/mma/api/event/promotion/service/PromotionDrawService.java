package my.mma.api.event.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.event.promotion.constant.PromotionPolicy;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.entity.PromotionWinner;
import my.mma.api.event.promotion.repository.GifticonRepository;
import my.mma.api.event.promotion.repository.PromotionRepository;
import my.mma.api.event.promotion.repository.PromotionWinnerRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.user.entity.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 프로모션 추첨(선정) 도메인 로직.
 * 스케줄러(PromotionDrawScheduler)와 관리자 버튼 양쪽에서 이 서비스를 호출한다.
 * (선정만 담당 — 실제 기프티콘 발송은 관리자 수동)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionDrawService {

    private final GifticonRepository gifticonRepository;
    private final BetCardRepository betCardRepository;
    private final PromotionWinnerRepository promotionWinnerRepository;
    private final PromotionRepository promotionRepository;
    private final S3ImgService s3ImgService;

    /**
     * 추첨 실행 (스케줄러 진입점)
     * 구현 시 지켜야 할 것:
     * 1) 멱등성 — 프로모션 재조회 후 isDrawn()이면 즉시 return
     * 2) 원자성 — 당첨자 선정 → PromotionWinner(PENDING) 생성
     */
    @Transactional
    @CacheEvict(value = "promotionDetail", key = "#promotionId")  // 당첨자 상세 반영
    public void draw(Long promotionId) {
        long startedAt = System.currentTimeMillis();
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        if (promotion.isDrawn()) {
            log.info("[추첨] 스킵(이미 추첨됨) promotionId={} drawnAt={}", promotionId, promotion.getDrawnAt());
            return;
        }
        log.info("[추첨] 시작 promotionId={} title='{}' 정원={}명 기간={}~{}",
                promotionId, promotion.getTitle(), promotion.getMaxWinnerCount(),
                promotion.getStartDate(), promotion.getEndDate());

        List<User> tickets = buildTickets(promotion);
        Collections.shuffle(tickets);
        List<User> winners = new ArrayList<>();
        Set<Long> picked = new HashSet<>();

        int dupSkipped = 0; // 이미 당첨된 유저의 잔여 티켓(중복 방지로 스킵된 수)
        for (User ticket : tickets) {
            if(winners.size() == promotion.getMaxWinnerCount())
                break;
            if(picked.add(ticket.getId()))
                winners.add(ticket);
            else
                dupSkipped++;
        }

        int max = promotion.getMaxWinnerCount();
        if (tickets.isEmpty())
            log.warn("[추첨] 참가자 0명 promotionId={} → 당첨자 없음, 기프티콘 전량 회수 예정", promotionId);
        else if (winners.size() < max)
            log.warn("[추첨] 참가자 < 정원 promotionId={} 당첨={}/{}명 (기프티콘 잉여 발생)",
                    promotionId, winners.size(), max);
        log.info("[추첨] 선정 promotionId={} 당첨={}/{}명 총티켓={}장 중복스킵={}장",
                promotionId, winners.size(), max, tickets.size(), dupSkipped);

        List<Gifticon> prizes = gifticonRepository.findByPromotionIdOrderByDisplayOrderAsc(promotion.getId());
        for(int i=0;i<winners.size();i++){
            prizes.get(i).assign();
            promotionWinnerRepository.save(PromotionWinner.of(promotion, winners.get(i), prizes.get(i)));
            if (log.isDebugEnabled())
                log.debug("[추첨] 배정 promotionId={} winnerId={} nickname='{}' ← 기프티콘#{} '{}'",
                        promotionId, winners.get(i).getId(), winners.get(i).getNickname(),
                        prizes.get(i).getDisplayOrder(), prizes.get(i).getName());
        }
        List<Gifticon> leftovers = prizes.stream().filter(g -> !g.isAssigned()).toList();
        if(!leftovers.isEmpty()){
            List<String> imageKeys = leftovers.stream()
                    .map(Gifticon::getImageKey).toList();
            log.info("[추첨] 잉여 기프티콘 회수 promotionId={} {}개 삭제 keys={}",
                    promotionId, leftovers.size(), imageKeys);
            gifticonRepository.deleteAll(leftovers);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    imageKeys.forEach(s3ImgService::deleteGifticonImg);
                }
            });
        }
        promotion.markDrawn();
        log.info("[추첨] 완료 promotionId={} 당첨={}명 잉여회수={}개 소요={}ms",
                promotionId, winners.size(), leftovers.size(), System.currentTimeMillis() - startedAt);
    }

    List<User> buildTickets(Promotion promotion) {
        Map<User, Long> countByUser = betCardRepository.findPromotionDurationBetCards(
                        promotion.getStartDate(), promotion.getEndDate()).stream()
                .collect(Collectors.groupingBy(
                        betCard -> betCard.getBet().getUser(),
                        Collectors.counting()
                ));

        List<User> tickets = new ArrayList<>();
        int cappedUsers = 0;   // 상한(ENTRY_CAP)에 걸린 인원
        long droppedByCap = 0; // 상한 초과로 티켓에 반영되지 못한 예측 수
        for (Map.Entry<User, Long> e : countByUser.entrySet()) {
            long raw = e.getValue();
            int entries = (int) Math.min(raw, PromotionPolicy.ENTRY_CAP);
            if (raw > PromotionPolicy.ENTRY_CAP) {
                cappedUsers++;
                droppedByCap += raw - PromotionPolicy.ENTRY_CAP;
            }
            for (int i = 0; i < entries; i++) tickets.add(e.getKey());
            if (log.isDebugEnabled())
                log.debug("[추첨] 응모가중 promotionId={} userId={} 예측={}회 → 티켓={}장{}",
                        promotion.getId(), e.getKey().getId(), raw, entries,
                        raw > PromotionPolicy.ENTRY_CAP ? " (상한 적용)" : "");
        }
        log.info("[추첨] 응모 집계 promotionId={} 참가자={}명 총티켓={}장 상한적용={}명 상한초과폐기={}건 (상한={}회)",
                promotion.getId(), countByUser.size(), tickets.size(),
                cappedUsers, droppedByCap, PromotionPolicy.ENTRY_CAP);
        return tickets;
    }

}
