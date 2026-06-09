package my.mma.api.global.ai.dto;

import lombok.Builder;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.CurrentFighterFightEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.FighterFightEventCardFighterDto;

import java.util.List;

/**
 * 이번 주 카드 개요. "어떤 매치업이 있고 시드(랭킹/전적)는 어떤가"만 가볍게 담는다.
 * 선수 상세(국적/신체/커리어 스탯/최근 전적)는 getFighterChatTool 로 따로 조회한다.
 */
@Builder
public record CurrentFightEventChatTool(
        String name,
        String location,
        List<CurrentFighterFightEventChatTool> fighterFightEvents
) {

    public static CurrentFightEventChatTool of(CurrentEventDto currentEvent) {
        return CurrentFightEventChatTool.builder()
                .name(currentEvent.getName())
                .location(currentEvent.getLocation())
                .fighterFightEvents(currentEvent.getFighterFightEvents().stream()
                        .map(CurrentFighterFightEventChatTool::of).toList())
                .build();
    }

    @Builder
    protected record CurrentFighterFightEventChatTool(
            String fightWeight,
            FighterChatTool fighter1,
            FighterChatTool fighter2,
            boolean title
    ) {
        protected static CurrentFighterFightEventChatTool of(CurrentFighterFightEventDto ffe) {
            return CurrentFighterFightEventChatTool.builder()
                    .fightWeight(ffe.getFightWeight())
                    .fighter1(FighterChatTool.of(ffe.getWinner()))
                    .fighter2(FighterChatTool.of(ffe.getLoser()))
                    .title(ffe.isTitle())
                    .build();
        }
    }

    @Builder
    protected record FighterChatTool(
            String name,
            String koreanName,
            Integer ranking,
            FightRecord record
    ) {
        protected static FighterChatTool of(FighterFightEventCardFighterDto fighter) {
            return FighterChatTool.builder()
                    .name(fighter.getName())
                    .koreanName(fighter.getKoreanName())
                    .ranking(fighter.getRanking())
                    .record(fighter.getRecord())
                    .build();
        }
    }
}
