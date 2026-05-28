package my.mma.fixture.dto.fighter;

import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fighter.entity.FightRecord;

public class FighterDtoFixture {

    public static FighterDto createFighterDto(Long fighterId, String headshotUrl) {
        return FighterDto.builder()
                .id(fighterId)
                .name("name-" + fighterId)
                .ranking(10)
                .record(FightRecord.builder()
                        .win(10)
                        .draw(0)
                        .loss(1)
                        .build())
//                .headshotUrl(headshotUrl)
                .build();
    }

}
