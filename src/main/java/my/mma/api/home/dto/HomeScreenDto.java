package my.mma.api.home.dto;

import lombok.*;
import my.mma.api.event.promotion.dto.HomePromotionsDto;
import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;
import my.mma.api.fightevent.dto.CurrentEventDto;

// Response
@Builder
public record HomeScreenDto(String eventName, CardStartDateTimeInfoDto mainCardDateTimeInfo,
                            String winnerBodyUrl, String loserBodyUrl,
                            String winnerName, String loserName,
                            String winnerKoreanName, String loserKoreanName,
                            String fightWeight, boolean title,
                            HomePromotionsDto activePromotions) {

    public static HomeScreenDto toDto(CurrentEventDto sfe, HomePromotionsDto activePromotions){
        CurrentEventDto.CurrentFighterFightEventDto firstFight = sfe.getFighterFightEvents().getFirst();
        return HomeScreenDto.builder()
                .eventName(sfe.getName())
                .mainCardDateTimeInfo(sfe.getMainCardDateTimeInfo())
                .winnerName(firstFight.getWinner().getName())
                .winnerBodyUrl(firstFight.getWinner().getBodyUrl())
                .winnerKoreanName(firstFight.getWinner().getKoreanName())
                .loserBodyUrl(firstFight.getLoser().getBodyUrl())
                .loserName(firstFight.getLoser().getName())
                .loserKoreanName(firstFight.getLoser().getKoreanName())
                .fightWeight(firstFight.getFightWeight())
                .title(firstFight.isTitle())
                .activePromotions(activePromotions)
                .build();
    }

}
