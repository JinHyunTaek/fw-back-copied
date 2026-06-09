package my.mma.api.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import my.mma.api.fighter.dto.CareerStatsDto;
import my.mma.api.fighter.entity.Country;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fightevent.dto.CurrentEventDto.FighterFightEventCardFighterDto;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.FightResult;
import my.mma.api.fightevent.entity.property.WinMethod;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * 선수 1명의 프로필 + 최근 전적.
 * careerStats 는 선수당 1회만 담아 (경기마다 중복 복제하던 구조를 제거) 토큰을 절약한다.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FighterProfileChatTool(
        String name,
        String koreanName,
        Integer ranking,
        FightRecord record,
        Country nationality,
        Integer heightCm,
        Integer reachCm,
        CareerStatsDto careerStats,
        List<RecentFightLine> recentFights
) {

    private static final int RECENT_FIGHT_LIMIT = 5;

    // 0(=크롤링 누락)은 미상으로 처리해 응답에서 생략되게 한다.
    private static Integer cmOrNull(int v) {
        return v > 0 ? v : null;
    }

    public static FighterProfileChatTool of(Long fighterId, List<FighterFightEvent> fights) {
        if (fights == null || fights.isEmpty()) return null;
        return build(resolveSelf(fights, fighter -> matchesId(fighter, fighterId)), fights);
    }

    /**
     * 전적 데이터가 없는 신규 선수용 — 카드의 기본 정보(이름/랭킹/전적/국적)만 담는다.
     * careerStats·recentFights 는 없으므로 LLM 이 일반 지식으로 추론하도록 둔다.
     */
    public static FighterProfileChatTool ofCard(FighterFightEventCardFighterDto fighter) {
        return FighterProfileChatTool.builder()
                .name(fighter.getName())
                .koreanName(fighter.getKoreanName())
                .ranking(fighter.getRanking())
                .record(fighter.getRecord())
                .nationality(fighter.getNationality())
                .heightCm(cmOrNull(fighter.getHeight()))
                .reachCm(cmOrNull(fighter.getReach()))
                .build();
    }

//    public static FighterProfileChatTool of(String fighterName, List<FighterFightEvent> fights) {
//        if (fights == null || fights.isEmpty()) return null;
//        return build(resolveSelf(fights, f -> matchesName(f, fighterName)), fights);
//    }

    // 조회 대상 선수 엔티티 (careerStats·신상은 어느 경기에서 가져와도 동일)
    private static Fighter resolveSelf(List<FighterFightEvent> fights, Predicate<Fighter> isSelf) {
        return fights.stream()
                .map(ffe -> isSelf.test(ffe.getWinner()) ? ffe.getWinner() : ffe.getLoser())
                .findFirst().orElse(null);
    }

    private static FighterProfileChatTool build(Fighter self, List<FighterFightEvent> fights) {
        if (self == null) return null;

        List<RecentFightLine> recent = fights.stream()
                .sorted(Comparator.comparing(
                        (FighterFightEvent f) -> f.getFightEvent().getEventDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_FIGHT_LIMIT)
                .map(f -> RecentFightLine.of(f, self.getId()))
                .toList();

        return FighterProfileChatTool.builder()
                .name(self.getName())
                .koreanName(self.getKoreanName())
                .ranking(self.getRanking())
                .record(self.getFightRecord())
                .nationality(self.getNationality())
                .heightCm(cmOrNull(self.getHeight()))
                .reachCm(cmOrNull(self.getReach()))
                .careerStats(self.getCareerStats() != null ? self.getCareerStats().toDto() : null)
                .recentFights(recent)
                .build();
    }

    private static boolean matchesId(Fighter fighter, Long id) {
        return fighter != null && id.equals(fighter.getId());
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RecentFightLine(
            String eventName,
            LocalDate eventDate,
            String fightWeight,
            String opponentName,
            String result,   // 조회 선수 기준: WIN | LOSS | DRAW | NC | UPCOMING
            WinMethod method,
            Integer round,
            boolean title
    ) {
        static RecentFightLine of(FighterFightEvent ffe, Long selfId) {
            boolean selfIsWinner = selfId.equals(ffe.getWinner().getId());
            Fighter opponent = selfIsWinner ? ffe.getLoser() : ffe.getWinner();
            FightResult fr = ffe.getFightResult();
            boolean completed = ffe.getFightEvent().isCompleted();

            String result;
            if (fr == null || !completed) result = "UPCOMING";
            else if (fr.isDraw()) result = "DRAW";
            else if (fr.isNc()) result = "NC";
            else result = selfIsWinner ? "WIN" : "LOSS";

            return RecentFightLine.builder()
                    .eventName(ffe.getFightEvent().getName())
                    .eventDate(ffe.getFightEvent().getEventDate())
                    .fightWeight(ffe.getFightWeight() != null ? ffe.getFightWeight().getDisplayName() : null)
                    .opponentName(opponent.getName())
                    .result(result)
                    .method(fr != null ? fr.getWinMethod() : null)
                    .round(fr != null ? fr.getRound() : null)
                    .title(ffe.isTitle())
                    .build();
        }
    }
}
