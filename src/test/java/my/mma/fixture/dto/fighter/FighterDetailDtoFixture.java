package my.mma.fixture.dto.fighter;

import my.mma.api.fighter.dto.FighterDetailDto;
import my.mma.api.fighter.entity.FightRecord;

import java.time.LocalDate;
import java.util.ArrayList;

public class FighterDetailDtoFixture {

    public static FighterDetailDto createFighterDetailDto(Long fighterId, String imgUrl) {
        return FighterDetailDto.builder()
                .id(fighterId)
                .name("name-" + fighterId)
                .nickname("nickname-" + fighterId)
                .ranking(10)
                .record(FightRecord.builder()
                        .win(10)
                        .draw(0)
                        .loss(1)
                        .build())
                .weight(155.5)
                .height(100)
                .birthday(LocalDate.of(2000,1,1))
                .reach(100)
                .fighterFightEvents(new ArrayList<>())
//                .bodyUrl(imgUrl)
                .alert(true)
                .build();
    }

}
