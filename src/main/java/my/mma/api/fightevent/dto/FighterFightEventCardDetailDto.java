package my.mma.api.fightevent.dto;

import lombok.Builder;


@Builder
public record FighterFightEventCardDetailDto(CurrentEventDto.FighterFightEventCardFighterDto winner,
                                             CurrentEventDto.FighterFightEventCardFighterDto loser,
                                             String fightWeight) {}