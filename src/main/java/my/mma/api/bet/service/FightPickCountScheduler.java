package my.mma.api.bet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.bet.entity.FightPickCount;
import my.mma.api.bet.repository.FightPickCountRepository;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.CurrentFighterFightEventDto;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

import static my.mma.api.global.redis.key.RedisKey.CURRENT_EVENT;

@Component
@RequiredArgsConstructor
@Slf4j
public class FightPickCountScheduler {

    private final RedisUtils<CurrentEventDto> currentEventRedisUtils;
    private final FightPickCountRepository fightPickCountRepository;

    @Loggable
    @Scheduled(cron = "0 0/20 * * * ?")
    public void synchronizeFightPickCount() {
        CurrentEventDto currentEvent = currentEventRedisUtils.getData(CURRENT_EVENT.getKey());
//        if (currentEvent != null && !CustomDateUtils.isThisWeekend(currentEvent.getDisplayDate())) {
        if (currentEvent != null) {
            Map<Long, CurrentFighterFightEventDto> currentFightMap = new HashMap<>();
            currentEvent.getFighterFightEvents()
                    .forEach(ffe -> currentFightMap.put(ffe.getId(), ffe));
            Set<FightPickCount> fightPickCounts = fightPickCountRepository
                    .findByFighterFightEventIdIn(currentFightMap.keySet());
            for (FightPickCount fightPickCount : fightPickCounts) {
                CurrentFighterFightEventDto currentFight = currentFightMap.get(fightPickCount.getFighterFightEvent().getId());
                currentFight.setFirstFighterBetCount(fightPickCount.getFirstFighterPickCount());
                currentFight.setLastFighterBetCount(fightPickCount.getLastFighterPickCount());
            }
            currentEventRedisUtils.saveData(CURRENT_EVENT.getKey(), currentEvent);
        }
    }


}
