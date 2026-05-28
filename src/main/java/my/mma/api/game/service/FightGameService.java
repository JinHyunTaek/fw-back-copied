package my.mma.api.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.game.dto.FightGameResponse;
import my.mma.api.game.dto.GameType;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


@Service
@Slf4j
@RequiredArgsConstructor
public class FightGameService implements GameService<FightGameResponse> {

    private final GameCacheService gameCacheService;

    @Override
    public GameType getType() {
        return GameType.FIGHT;
    }

    @Override
    public List<FightGameResponse> generateGame(boolean isNormal) {
        Set<FighterFightEvent> fights = isNormal
                ? gameCacheService.todayFightGamesNormal()
                : gameCacheService.todayFightGamesHard();
        List<FighterFightEvent> shuffled = new ArrayList<>(fights);
        Collections.shuffle(shuffled);
        return FightGameResponse.of(shuffled);
    }

}
