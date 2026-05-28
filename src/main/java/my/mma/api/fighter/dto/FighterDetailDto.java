package my.mma.api.fighter.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import my.mma.api.fightevent.dto.FightEventDto;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fightevent.dto.FightEventDto.FighterFightEventDto;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class FighterDetailDto extends FighterDto {

    private int height;
    protected Double weight;
    private LocalDate birthday;
    private int reach;
    private boolean alert;
//    private String bodyUrl;
    private String nickname;
    private double avgRating;
    private int myRating;
    private List<FighterFightEventDto> fighterFightEvents;

    public static FighterDetailDto toDto(Fighter fighter, List<FighterFightEventDto> fighterFightEvents,
                                         boolean alert, int myRating) {
        return FighterDto.toDto(fighter, builder()
                .height(fighter.getHeight())
                .weight(fighter.getWeight())
                .birthday(fighter.getBirthday())
                .reach(fighter.getReach())
                .alert(alert)
                .nickname(fighter.getNickname())
//                .bodyUrl(bodyUrl)
                .avgRating(fighter.getAvgRating())
                .myRating(myRating)
                .fighterFightEvents(fighterFightEvents)
        );
    }

}
