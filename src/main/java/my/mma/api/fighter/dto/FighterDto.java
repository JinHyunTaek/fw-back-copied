package my.mma.api.fighter.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import my.mma.api.fighter.entity.Country;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class FighterDto {

    private Long id;
    private String name;
    private String koreanName;
    private Integer ranking;
    private FightRecord record;
    private Country nationality;
    @Setter
    private String headshotUrl;

    public static FighterDto toDto(Fighter fighter){
        return toDto(fighter, builder());
    }

    public static <T extends FighterDto> T toDto(Fighter fighter, FighterDto.FighterDtoBuilder<T, ?> builder) {
        return builder
                .id(fighter.getId())
                .name(fighter.getName())
                .koreanName(fighter.getKoreanName())
                .ranking(fighter.getRanking())
                .record(fighter.getFightRecord())
                .nationality(fighter.getNationality())
                .build();
    }

}
