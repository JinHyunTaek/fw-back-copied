package my.mma.api.user.dto;

import lombok.Builder;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fightevent.dto.FightEventDto;

import java.util.List;

@Builder
public record UserProfileDto(UserBetRecord userBetRecord, List<FighterDto> alertFighters, List<FightEventDto.FighterFightEventDto> alertEvents) {
}
