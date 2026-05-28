package my.mma.api.fightevent.dto;

import lombok.*;
import my.mma.api.fightevent.entity.property.FightResult;
import my.mma.api.fightevent.entity.property.WinMethod;

import java.time.Duration;

@Builder
public record FightResultDto(WinMethod winMethod, int round, Duration fightDuration, String description,
                             boolean draw, boolean nc) {

    public static FightResultDto toDto(FightResult result){
        return FightResultDto.builder()
                .fightDuration(result.getFightDuration())
                .round(result.getRound())
                .winMethod(result.getWinMethod())
                .description(result.getWinDescription())
                .draw(result.isDraw())
                .nc(result.isNc())
                .build();
    }

}
