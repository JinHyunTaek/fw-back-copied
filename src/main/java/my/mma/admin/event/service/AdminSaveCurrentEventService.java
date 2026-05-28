package my.mma.admin.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.bet.entity.FightPickCount;
import my.mma.api.bet.repository.FightPickCountRepository;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.CurrentFighterFightEventDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static my.mma.api.global.redis.key.RedisKey.CURRENT_EVENT;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminSaveCurrentEventService {

    private final FightEventRepository fightEventRepository;
    private final FighterFightEventRepository fighterFightEventRepository;
    private final RedisUtils<CurrentEventDto> redisUtils;
    private final FightPickCountRepository fightPickCountRepository;

    /**
     * case 1. 같은 currentEvent를 갱신 (카드 내용이 달라질 수도 있음)
     * case 2. 기존 경기 종료 후 아예 새로운 이벤트로 갱신
     */
    @Loggable
    @Transactional
    public void syncStreamFightEvent() {
        CurrentEventDto existing = redisUtils.getData(CURRENT_EVENT.getKey());
        buildCurrentEventFromDb(existing);
    }

    private void buildCurrentEventFromDb(CurrentEventDto existing) {
        List<Long> newFfeIds = new ArrayList<>();
        FightEvent fightEvent = fightEventRepository.findFirstByCompletedIsFalseOrderByEventDateAsc();
        CurrentEventDto built = CurrentEventDto.toDto(fightEvent);
        boolean isNewEvent = existing == null || existing.getId() != built.getId();
        built.getFighterFightEvents().forEach(
                currFfe -> {
                    if (isNewEvent) {
                        newFfeIds.add(currFfe.getId());
                    } else {
                        CurrentFighterFightEventDto existingCard = findExistingCard(existing, currFfe);
                        if (existingCard == null) {
                            // 새롭게 추가된 카드
                            newFfeIds.add(currFfe.getId());
                        } else {
                            // 기존 카드
                            currFfe.setFirstFighterBetCount(existingCard.getFirstFighterBetCount());
                            currFfe.setLastFighterBetCount(existingCard.getLastFighterBetCount());
                        }
                    }
//                    currFfe.getWinner().setHeadshotUrl(s3Service.generateFighterHeadshotUrl(currFfe.getWinner().getName()));
//                    currFfe.getLoser().setHeadshotUrl(s3Service.generateFighterHeadshotUrl(currFfe.getLoser().getName()));
//                    currFfe.getWinner().setBodyUrl(s3Service.generateFighterBodyUrl(currFfe.getWinner().getName()));
//                    currFfe.getLoser().setBodyUrl(s3Service.generateFighterBodyUrl(currFfe.getLoser().getName()));
                }
        );
        redisUtils.saveData(CURRENT_EVENT.getKey(), built);
        initFightPickCount(newFfeIds);
    }

    private void initFightPickCount(List<Long> ffeIds) {
        if (ffeIds.isEmpty())
            return;
        List<FighterFightEvent> fights = fighterFightEventRepository.findAllById(ffeIds);
        Set<Long> existingFfeIds = fightPickCountRepository.findFfeIdsByFfeIds(ffeIds);
        List<FightPickCount> fightPickCounts = new ArrayList<>();

        for (FighterFightEvent fight : fights) {
            if (!existingFfeIds.contains(fight.getId()))
                fightPickCounts.add(FightPickCount.builder()
                        .fighterFightEvent(fight)
                        .build());
        }
        fightPickCountRepository.saveAll(fightPickCounts);
    }

    private CurrentFighterFightEventDto findExistingCard(
            CurrentEventDto existingStreamFightEvent,
            CurrentFighterFightEventDto newCard) {
        for (CurrentFighterFightEventDto existingCard : existingStreamFightEvent.getFighterFightEvents()) {
            if ((newCard.getWinner().getId().equals(existingCard.getWinner().getId())
                    && newCard.getLoser().getId().equals(existingCard.getLoser().getId()))
                    || (newCard.getWinner().getId().equals(existingCard.getLoser().getId())
                    && newCard.getLoser().getId().equals(existingCard.getWinner().getId()))) {
                return existingCard;
            }
        }
        return null;
    }

}
