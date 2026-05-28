package my.mma.fixture.dto.fightevent;

import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fightevent.dto.FighterFightEventCardDetailDto;
import my.mma.api.fightevent.dto.CurrentEventDto.FighterFightEventCardFighterDto;

import java.time.LocalDate;

public class FighterFightEventCardDetailDtoFixture {

    public static FighterFightEventCardDetailDto createFighterFightEventCardDetailDto(){
        return FighterFightEventCardDetailDto.builder()
                .winner(createFighterFightEventCardFighterDto(1L))
                .loser(createFighterFightEventCardFighterDto(2L))
                .fightWeight("fight-weight")
                .build();
    }

    public static FighterFightEventCardFighterDto createFighterFightEventCardFighterDto(long id){
        return FighterFightEventCardFighterDto.builder()
                .id(id)
                .name("name-"+id)
                .birthday(LocalDate.of(2000,10,10))
                .height(100)
                .weight(155.5)
                .reach(10)
                .record(FightRecord.builder()
                        .win(10)
                        .loss(1)
                        .draw(0)
                        .build())
                .build();
    }

}
