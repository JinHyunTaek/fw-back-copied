package my.mma.api.bet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.bet.dto.BetDeleteResponse;
import my.mma.api.bet.dto.BetResponse;
import my.mma.api.bet.dto.SingleBetRequest;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.entity.BetPrediction;
import my.mma.api.bet.event.FightPickEvent;
import my.mma.api.bet.event.FightPickEventPublisher;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.global.utils.CustomDateUtils;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import static my.mma.api.bet.constant.PredictionPolicy.ENTRY_FEE;
import static my.mma.api.bet.dto.BetResponse.SingleBetResponse;
import static my.mma.api.global.redis.prefix.RedisKeyPrefix.BET_CANCEL_COUNT_PREFIX;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BetService {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final BetCardRepository betCardRepository;
    private final FightEventRepository fightEventRepository;
    private final FighterFightEventRepository fighterFightEventRepository;
    private final RedisUtils<CurrentEventDto> currentEventRedisUtils;
    private final FightPickEventPublisher fightPickEventPublisher;
    private final StringRedisTemplate betCancelCountRedisTemplate;

    /**
     * 이벤트당 예측 가능 횟수. 운영은 3이며, 부하 테스트에서는 같은 계정으로 반복 요청해야 해
     * loadtest 프로파일에서만 크게 잡는다(기본값이 3이라 설정이 없으면 운영 동작 그대로다).
     */
    @Value("${bet.available-count:3}")
    private int betAvailableCount;

    protected static final int BET_CANCEL_AVAILABLE_COUNT = 3;

    @Loggable
    @Transactional
    public Integer predict(String email, SingleBetRequest betRequest) {
        User user = userRepository.findByEmailWithLock(email);
        CurrentEventDto currentEvent = currentEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
        if (currentEvent == null || currentEvent.getId() != betRequest.eventId())
            throw new CustomException(ErrorCode.NO_SUCH_EVENT_FOUND_400);
        LocalDateTime eventStartDateTime = getCurrentEventStartDateTime(currentEvent);
        if (CustomDateUtils.isThisWeekend(currentEvent.getDisplayDate()) ||
                (eventStartDateTime != null && LocalDateTime.now().isAfter((eventStartDateTime))))
            throw new CustomException(ErrorCode.BET_NOT_AVAILABLE_DATE_403);
        if (betRequest.seedPoint() % ENTRY_FEE.getPoint() != 0)
            throw new CustomException(ErrorCode.INVALID_SEEDPOINT_400);
        if (user.getPoint() < betRequest.seedPoint())
            throw new CustomException(ErrorCode.LOW_USER_POINT_400);
        if (betCardRepository.countByUserIdAndFightEventId(user.getId(), currentEvent.getId())
                + betRequest.singleBetCards().size() > betAvailableCount) {
            throw new CustomException(ErrorCode.BET_LIMIT_EXCEED_403);
        }

        FightEvent fightEvent = fightEventRepository.findByIdWithFfes(betRequest.eventId()).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_EVENT_FOUND_400)
        );
        Bet bet = betRequest.toEntity(user, fightEvent);
        List<FightPickEvent> pickEvents = new ArrayList<>();
        for (SingleBetRequest.SingleBetCardRequest sbc : betRequest.singleBetCards()) {
            FighterFightEvent ffe = extractFighterFightEventById(sbc.fighterFightEventId());
            if (ffe.isCanceled()) {
                throw new CustomException(ErrorCode.FIGHT_CANCELED_400);
            }
            BetCard betCard = sbc.toEntity(ffe, bet);
            bet.addBetCard(betCard);
            if (!sbc.betPrediction().isDraw()) {
                pickEvents.add(FightPickEvent.of(ffe.getId(), pickSide(sbc.betPrediction(), ffe), 1));
            }
        }
        betRepository.save(bet);
        user.updatePoint(user.getPoint() - betRequest.seedPoint());
        // 픽 카운트 반영은 마지막에 한 번만. 락 획득 순서·보유 시간은 publisher 뒤편에서 관리한다.
        fightPickEventPublisher.publish(pickEvents);
        return user.getPoint();
    }

    public BetResponse predictionHistory(String email, Long eventId) {
        User user = extractUserByEmail(email);
        //  eventId & 특정 userId에 필더링된 bet(s) data
        String eventName = fightEventRepository.findById(eventId).map(FightEvent::getName)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_EVENT_FOUND_400));
        List<Bet> bets = betRepository.findByFightEventIdAndUserId(eventId, user.getId());
        return BetResponse.builder()
                .eventName(eventName)
                .singleBets(bets.stream().map(SingleBetResponse::toDto).toList())
                .build();
    }

    @Loggable
    @Transactional
    public BetDeleteResponse deletePrediction(String email, Long betId) {
        CurrentEventDto currentEvent = currentEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
        LocalDateTime eventStartDateTime = getCurrentEventStartDateTime(currentEvent);
        if (CustomDateUtils.isThisWeekend(currentEvent.getDisplayDate()) ||
                (eventStartDateTime != null && LocalDateTime.now().isAfter((eventStartDateTime))))
            throw new CustomException(ErrorCode.BET_NOT_AVAILABLE_DATE_403);
        User user = extractUserByEmail(email);
        Bet bet = betRepository.findByIdAndUserId(betId, user.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND)
        );
        String key = getBetCancelCountKey(currentEvent.getId(), user.getId());
        Duration ttl = Duration.between(LocalDateTime.now(), currentEvent.getDisplayDate().atStartOfDay().plusDays(2));
        Long count = betCancelCountRedisTemplate.opsForValue().increment(key);
        betCancelCountRedisTemplate.expire(key, ttl);
        if (count > BET_CANCEL_AVAILABLE_COUNT) {
            betCancelCountRedisTemplate.opsForValue().decrement(key);
            throw new CustomException(ErrorCode.BET_CANCEL_LIMIT_EXCEED_403);
        }
        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    betCancelCountRedisTemplate.opsForValue().decrement(key);
                }
            }
        });
        List<FightPickEvent> pickEvents = new ArrayList<>();
        for (BetCard betCard : bet.getBetCards()) {
            FighterFightEvent betCardFfe = betCard.getFighterFightEvent();
            if (!betCard.getBetPrediction().isDraw()) {
                pickEvents.add(FightPickEvent.of(
                        betCardFfe.getId(), pickSide(betCard.getBetPrediction(), betCardFfe), -1));
            }
        }
        betRepository.deleteById(betId);
        List<Bet> userBets = betRepository.findByFightEventIdAndUserId(currentEvent.getId(),
                user.getId());
        user.updatePoint(user.getPoint() + bet.getSeedPoint());
        fightPickEventPublisher.publish(pickEvents);
        return new BetDeleteResponse(user.getPoint(), BetResponse.builder()
                .eventName(currentEvent.getName())
                .singleBets(userBets.stream().map(SingleBetResponse::toDto).toList())
                .build());
    }

    private LocalDateTime getCurrentEventStartDateTime(CurrentEventDto currentEvent) {
        CardStartDateTimeInfoDto startDateTimeInfoDto = currentEvent.getEarlyCardDateTimeInfo() != null ? currentEvent.getEarlyCardDateTimeInfo()
                : currentEvent.getPrelimCardDateTimeInfo();
        if (startDateTimeInfoDto == null) {
            startDateTimeInfoDto = currentEvent.getMainCardDateTimeInfo();
        }
        if (startDateTimeInfoDto == null) {
            return null;
        }
        return LocalDateTime.of(startDateTimeInfoDto.date(), startDateTimeInfoDto.time());
    }

    private User extractUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400)
        );
    }

    /** 예측한 승자가 카드의 첫 번째(winner) 선수인지에 따라 증감 대상 컬럼이 갈린다. */
    private FightPickEvent.PickSide pickSide(BetPrediction prediction, FighterFightEvent ffe) {
        return prediction.getMyWinnerId().equals(ffe.getWinner().getId())
                ? FightPickEvent.PickSide.FIRST
                : FightPickEvent.PickSide.LAST;
    }

    private FighterFightEvent extractFighterFightEventById(Long ffeId) {
        return fighterFightEventRepository.findById(ffeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_FIGHT_FOUND_400));
    }

    private String getBetCancelCountKey(Long eventId, Long userId) {
        return BET_CANCEL_COUNT_PREFIX.getPrefix() + eventId + ":" + userId;
    }

}
