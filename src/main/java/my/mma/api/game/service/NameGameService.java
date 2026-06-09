package my.mma.api.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.fighter.dto.RankersPerCategory;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.game.dto.*;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.stereotype.Service;

import java.util.*;

import static my.mma.api.fighter.dto.RankersPerCategory.RankerDto;
import static my.mma.api.fighter.dto.RankersPerCategory.RankingCategory;
import static my.mma.api.global.redis.prefix.RedisKeyPrefix.RANKERS_KEY_PREFIX;

@Service
@Slf4j
@RequiredArgsConstructor
public class NameGameService implements GameService<NameGameResponse>{

    private final RedisUtils<RankersPerCategory> rankersRedisUtils;
    private final FighterRepository fighterRepository;
    private final FighterFightEventRepository fighterFightEventRepository;
    private final GameCacheService gameCacheService;

    @Override
    public GameType getType() {
        return GameType.NAME;
    }

    @Override
    public List<NameGameResponse> generateGame(boolean isNormal) {
        Set<FighterNamePair> names = isNormal
                ? gameCacheService.popularFighterNamePairs()
                : gameCacheService.allFighterNamePairs();
        List<NameGameResponse> nameQuestions = new ArrayList<>();
        Arrays.stream(NameGameCategory.values()).forEach(
                nameGameCategory -> nameQuestions.add(generateNameQuestion(nameGameCategory, names)
                ));
        return nameQuestions;
    }

    NameGameResponse generateNameQuestion(NameGameCategory category, Set<FighterNamePair> names) {
        return switch (category) {
//            case HEADSHOT -> generateHeadshotQuestion(new ArrayList<>(names));
//            case BODY -> generateBodyQuestion(new ArrayList<>(names));
            case COUNTRY -> generateCountryQuestion(new ArrayList<>(names));
            case OPPONENT -> generateOpponentQuestion(new ArrayList<>(names));
            case NICKNAME -> generateNicknameQuestion(new ArrayList<>(names));
            case RECORD -> generateRecordQuestion(new ArrayList<>(names));
            case RANKING -> generateRankingQuestion();
        };
    }

    private NameGameResponse generateCountryQuestion(List<FighterNamePair> names) {
        List<String> nameList = names.stream().map(FighterNamePair::name).toList();
        List<Fighter> fighters = fighterRepository.findByNameInAndNationalityIsNotNull(nameList);
        Collections.shuffle(fighters);
        Fighter answer = fighters.getFirst();
        List<String> wrongSelections = fighters.stream()
                .filter(f -> f.getNationality() != answer.getNationality())
                .limit(3)
                .map(Fighter::getKoreanName)
                .toList();

        return NameGameResponse.builder()
                .answer(answer.getKoreanName())
                .nationality(answer.getNationality())
                .nameGameCategory(NameGameCategory.COUNTRY)
                .wrongSelections(new ArrayList<>(wrongSelections))
                .build();
    }

    private NameGameResponse generateOpponentQuestion(List<FighterNamePair> names) {
        Collections.shuffle(names);
        for (FighterNamePair currNamePair : names) {
            List<FighterFightEvent> wins = fighterFightEventRepository.findCompletedByWinnerName(currNamePair.name());
            if (wins.size() < 2) continue;

            Collections.shuffle(wins);
            List<String> opponents = wins.subList(0, 2).stream()
                    .map(ffe -> ffe.getLoser().getKoreanName() != null
                            ? ffe.getLoser().getKoreanName() : ffe.getLoser().getName())
                    .toList();

            List<String> wrongSelections = names.stream()
                    .map(FighterNamePair::koreanName)
                    .filter(korName -> !korName.equals(currNamePair.koreanName()))
                    .limit(3)
                    .toList();

            return NameGameResponse.builder()
                    .answer(currNamePair.koreanName())
                    .opponents(opponents)
                    .nameGameCategory(NameGameCategory.OPPONENT)
                    .wrongSelections(new ArrayList<>(wrongSelections))
                    .build();
        }
        throw new CustomException(ErrorCode.SERVER_ERROR_500, "opponent question generation error");
    }

    private NameGameResponse generateRankingQuestion() {
        // random value returns certain division's index
        long count = Arrays.stream(RankingCategory.values()).count();
        Random random = new Random();
        int randomIndex = random.nextInt((int) count);
        RankingCategory randomRankingCategory = RankingCategory.values()[randomIndex];
        RankersPerCategory rankersPerCategory = rankersRedisUtils.getData(
                RANKERS_KEY_PREFIX.getPrefix() + randomRankingCategory.name());
        List<RankerDto> rankerDtos = rankersPerCategory.rankers();
        Collections.shuffle(rankerDtos);
        RankerDto randomRanker = rankerDtos.getFirst();
        return NameGameResponse.builder()
                .answer(randomRanker.getKoreanName() != null ? randomRanker.getKoreanName() : randomRanker.getName())
                .ranking(randomRanker.getRanking())
                .rankingCategory(randomRanker.getCategory())
                .nameGameCategory(NameGameCategory.RANKING)
                .wrongSelections(new ArrayList<>(rankerDtos.subList(1, 4).stream().map(
                        wrong -> wrong.getKoreanName() != null ? wrong.getKoreanName() : wrong.getName()
                ).toList()))
                .build();
    }

    private NameGameResponse generateRecordQuestion(List<FighterNamePair> names) {
        Collections.shuffle(names);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).name();
            Optional<Fighter> fighterOptional = fighterRepository.findByNameAndFightRecordIsNotNull(name);
            if (fighterOptional.isPresent()) {
                Fighter fighter = fighterOptional.get();
                return NameGameResponse.builder()
                        .answer(fighter.getKoreanName())
                        .fightRecord(fighter.getFightRecord())
                        .nameGameCategory(NameGameCategory.RECORD)
                        .wrongSelections(new ArrayList<>(names.stream().map(FighterNamePair::koreanName)
                                .toList().subList(i + 1, Math.min(i + 4, names.size()))))
                        .build();
            }
        }
        throw new CustomException(ErrorCode.SERVER_ERROR_500, "record question generation error");
    }

    private NameGameResponse generateNicknameQuestion(List<FighterNamePair> names) {
        Collections.shuffle(names);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).name();
            Optional<Fighter> fighterOptional = fighterRepository.findByNameAndNicknameIsNotNull(name);
            if (fighterOptional.isPresent()) {
                Fighter fighter = fighterOptional.get();
                return NameGameResponse.builder()
                        .answer(fighter.getKoreanName())
                        .nickname(fighter.getNickname())
                        .wrongSelections(new ArrayList<>(names.stream().map(FighterNamePair::koreanName)
                                .toList().subList(i + 1, Math.min(i + 4, names.size()))))
                        .nameGameCategory(NameGameCategory.NICKNAME)
                        .build();
            }
        }
        throw new CustomException(ErrorCode.SERVER_ERROR_500, "nickname question generation error");
    }

}
