package my.mma.api.bet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.bet.entity.FightPickCount;
import my.mma.api.bet.repository.FightPickCountRepository;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.CurrentFighterFightEventDto;
import my.mma.api.fightevent.store.CurrentEventStore;
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

    private final CurrentEventStore currentEventStore;
    private final FightPickCountRepository fightPickCountRepository;

    @Loggable
    @Scheduled(cron = "0 0/20 * * * ?")
    public void synchronizeFightPickCount() {
        currentEventStore.mutate(event -> {
            Map<Long, CurrentFighterFightEventDto> currentFightMap = new HashMap<>();
            event.getFighterFightEvents()
                    .forEach(ffe -> currentFightMap.put(ffe.getId(), ffe));
            Set<FightPickCount> fightPickCounts = fightPickCountRepository
                    .findByFighterFightEventIdIn(currentFightMap.keySet());
            for (FightPickCount fightPickCount : fightPickCounts) {
                CurrentFighterFightEventDto currentFight = currentFightMap.get(fightPickCount.getFighterFightEvent().getId());
                currentFight.setFirstFighterBetCount(fightPickCount.getFirstFighterPickCount());
                currentFight.setLastFighterBetCount(fightPickCount.getLastFighterPickCount());
            }
            return event;
        });
    }


}
