package my.mma.admin.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.entity.BetPrediction;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.WinMethod;
import my.mma.api.global.fcm.AdminPushNotificationService;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.user.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static my.mma.api.bet.constant.PredictionPolicy.*;
import static my.mma.api.fightevent.entity.property.WinMethod.*;

/**
 * case 1 : 만약 cancel 된 경기가 단일 배팅 => 배팅 포인트 환불
 * case 2 : 만약 cancel 된 경기가 조합 배팅에 포함될 때 => 해당 경기 제외하여 포인트 정산 (환불 x)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BetPointHandler {

    private final BetRepository betRepository;
    private final AdminPushNotificationService adminPushNotificationService;

    /**
     * 1. streamFightEventredisUtils에서 현재 이벤트 조회
     * 2. fightEventRepository에서 해당 이벤트 추출
     * 3. 만약 ffe canceled -> betRepo 조회
     */
    @Loggable
    @Transactional
    public void handleUserBets(Long eventId) {
        processBets(betRepository.findByEventIdWithBetCards(eventId));
    }

    @Loggable
    @Transactional
    public void retryUnsettledBets() {
        processBets(betRepository.findAllUnsettledWithDetails());
    }

    private void processBets(List<Bet> bets) {
        for (Bet userBet : bets) {
            if (Boolean.TRUE.equals(userBet.getSettled()))
                continue;
            try {
                User user = userBet.getUser();
                if (shouldRefund(userBet.getBetCards())) {
                    user.updatePoint(user.getPoint() + userBet.getSeedPoint());
                    userBet.markRefunded();
                    userBet.updateSettledToTrue();
                } else {
                    boolean isCanceledOrNcIn = false;
                    boolean allSucceed = true;
                    int accumulatedScore = 0;
                    int totalOptionScore = 0;
                    int totalOptions = 0;
                    List<BetCard> userBetCards = userBet.getBetCards();
                    for (BetCard userBetCard : userBetCards) {
                        boolean fourOptionsCorrect = true;

                        FighterFightEvent ffe = userBetCard.getFighterFightEvent();

                        if (ffe.isCanceled() || ffe.getFightResult().isNc()) {
                            isCanceledOrNcIn = true;
                            continue;
                        }

                        BetPrediction prediction = userBetCard.getBetPrediction();
                        if (prediction.isFotN()) {
                            if (ffe.isFotN()) {
                                accumulatedScore += OTN_HIT.getPoint();
                                totalOptions++;
                            } else {
                                allSucceed = false;
                                break;
                            }
                        }
                        if (prediction.isPotN()) {
                            if (ffe.isPotN()) {
                                accumulatedScore += OTN_HIT.getPoint();
                                totalOptions++;
                            } else {
                                allSucceed = false;
                                break;
                            }
                        }
                        if (!prediction.isFotN() && !prediction.isPotN())
                            fourOptionsCorrect = false;

                        if (prediction.isDraw()) {
                            if (ffe.getFightResult().isDraw()) {
                                totalOptions++;
                                accumulatedScore += DRAW_HIT.getPoint();
                                continue;
                            } else {
                                allSucceed = false;
                                break;
                            }
                        }
                        if (ffe.getFightResult().isDraw()) {
                            allSucceed = false;
                            break;
                        }

                        // (무승부 배팅 아닌 경우) 배팅 성공 1차 전제 조건 (승패자 맞추기)
                        if (!prediction.getMyWinnerId().equals(ffe.getWinner().getId())) {
                            allSucceed = false;
                            break;
                        } else {
                            totalOptions++;
                            accumulatedScore += WINNER_HIT.getPoint();
                        }
                        // bet winMethod에 포함되지 않으므로 승자만 맞추면 2배
                        if (ffe.getFightResult().getWinMethod().equals(DQ))
                            continue;

                        // winMethod 배팅한 경우
                        WinMethod userWinMethodPick = prediction.getWinMethod();
                        if (userWinMethodPick != null) {
                            if (isDec(ffe.getFightResult().getWinMethod())) {
                                if (userWinMethodPick.equals(WinMethod.DEC)) {
                                    totalOptions++;
                                    accumulatedScore += WIN_METHOD_DEC_HIT.getPoint();
                                } else {
                                    allSucceed = false;
                                    break;
                                }
                            } else {
                                if (userWinMethodPick.equals(ffe.getFightResult().getWinMethod())) {
                                    totalOptions++;
                                    accumulatedScore += WIN_METHOD_FINISH_HIT.getPoint();
                                } else {
                                    allSucceed = false;
                                    break;
                                }
                            }
                        } else {
                            fourOptionsCorrect = false;
                        }

                        // finishRound 배팅한 경우
                        if (prediction.getFinishRound() != null) {
                            if (ffe.getFightResult().getRound() != prediction.getFinishRound()
                                    || isDec(ffe.getFightResult().getWinMethod())) {
                                allSucceed = false;
                                break;
                            } else {
                                totalOptions++;
                                accumulatedScore += FINISH_ROUND_HIT.getPoint();
                            }
                        } else {
                            fourOptionsCorrect = false;
                        }

                        if (fourOptionsCorrect)
                            totalOptionScore += FULL_OPTION_BONUS.getPoint();
                    }
                    if (allSucceed) {
                        if (userBetCards.size() > 1 && !isCanceledOrNcIn) {
                            accumulatedScore += (totalOptions * COMBO_BONUS_PER_OPTION.getPoint());
                        }
                        user.accumulateEarnedBetSucceedPoint(
                                accumulatedScore + totalOptionScore);
                    }
                    userBet.updateSucceed(allSucceed);
                    userBet.updateSettledToTrue();
                }
            } catch (Exception e) {
                userBet.updateSettledToFalse();
                log.error("Bet handler exception. betId={}", userBet.getId(), e);
                adminPushNotificationService.sendNotificationToAdmin("exception while handling betPoint");
            }
        }
    }

    private boolean shouldRefund(List<BetCard> betCards) {
        // 배팅 카드 중 하나라도 cancel, NC 아닌 경우, 환불 x (모두 cancel or nc => 환불 o)
        return betCards.stream().allMatch(card ->
                card.getFighterFightEvent().isCanceled() ||
                        card.getFighterFightEvent().getFightResult().isNc()
        );
    }

    private boolean isDec(WinMethod fightResultWinMethod) {
        return fightResultWinMethod.equals(U_DEC) || fightResultWinMethod.equals(M_DEC) || fightResultWinMethod.equals(S_DEC);
    }

}
