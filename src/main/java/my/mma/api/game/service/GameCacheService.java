package my.mma.api.game.service;

import lombok.RequiredArgsConstructor;
import my.mma.api.fighter.dto.RankersPerCategory;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fighter.repository.UserFighterRatingRepository;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.WinMethod;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.game.dto.FighterNamePair;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static my.mma.api.fighter.dto.RankersPerCategory.RankingCategory;
import static my.mma.api.global.redis.prefix.RedisKeyPrefix.RANKERS_KEY_PREFIX;

@RequiredArgsConstructor
@Service
public class GameCacheService {

    private final FighterFightEventRepository fighterFightEventRepository;
    private final FighterRepository fighterRepository;
    private final UserFighterRatingRepository fighterRatingRepository;
    private final RedisUtils<RankersPerCategory> rankersRedisUtils;

    @Cacheable(value = "popularFighterNamePairs", sync = true)
    public Set<FighterNamePair> popularFighterNamePairs() {
        return fetchPopularFighterNamePairs();
    }

    @Cacheable(value = "allFighterNamePairs", sync = true)
    public Set<FighterNamePair> allFighterNamePairs() {
        List<FighterNamePair> all = new ArrayList<>(fighterRepository.findEveryNamePairs());
        Collections.shuffle(all);
        return all.stream().limit(600).collect(Collectors.toSet());
    }

    @Cacheable(value = "todayFightGamesNormal", sync = true)
    public Set<FighterFightEvent> todayFightGamesNormal() {
        Set<String> names = fetchPopularFighterNamePairs().stream()
                .map(FighterNamePair::name)
                .collect(Collectors.toSet());
        return fighterFightEventRepository.findCompletedForNormalGame(names, gameWinMethods());
    }

    @Cacheable(value = "todayFightGamesHard", sync = true)
    public Set<FighterFightEvent> todayFightGamesHard() {
        List<FighterFightEvent> all = fighterFightEventRepository.findCompletedForHardGame(gameWinMethods());
        Collections.shuffle(all);
        return all.stream().limit(500).collect(Collectors.toSet());
    }

    private Set<FighterNamePair> fetchPopularFighterNamePairs() {
        Set<FighterNamePair> topRating = new HashSet<>(
                fighterRatingRepository.findTop100ByRating(PageRequest.of(0, 100)));
        Set<FighterNamePair> rankerPairs = new HashSet<>();
        for (RankingCategory category : RankingCategory.values()) {
            RankersPerCategory rankersPerCategory = rankersRedisUtils.getData(
                    RANKERS_KEY_PREFIX.getPrefix() + category.name());
            if (rankersPerCategory != null) {
                rankersPerCategory.rankers().stream()
                        .filter(r -> r.getKoreanName() != null)
                        .map(r -> new FighterNamePair(r.getName(), r.getKoreanName()))
                        .forEach(rankerPairs::add);
            }
        }
        return Stream.of(topRating, rankerPairs).flatMap(Set::stream).collect(Collectors.toSet());
    }

    private List<WinMethod> gameWinMethods() {
        return List.of(WinMethod.U_DEC, WinMethod.M_DEC, WinMethod.S_DEC, WinMethod.SUB, WinMethod.KO_TKO);
    }

}