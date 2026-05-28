package my.mma.fixture.entity.fighter;

import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;

import java.time.LocalDate;
import java.util.Random;

public class FighterFixture {

    public static final String NAME_PREFIX = "name-";
    public static final String NICKNAME_PREFIX = "nickname-";

    public static Fighter createFighter() {
        return Fighter.builder()
                .id(1L)
                .name(NAME_PREFIX + 1)
                .nickname(NICKNAME_PREFIX + 1)
                .height(180)
                .weight(155.5)
                .reach(100)
                .ranking(new Random().nextInt(15))
                .birthday(LocalDate.of(1990, 1, 1))
                .fightRecord(FightRecord.builder()
                        .win(10)
                        .draw(0)
                        .loss(1)
                        .build())
                .build();
    }

    public static Fighter createFighterWithNumber(int num) {
        return Fighter.builder()
                .id((long) num)
                .name(NAME_PREFIX + num)
                .nickname(NICKNAME_PREFIX + num)
                .height(180)
                .weight(155.5)
                .reach(100)
                .ranking(new Random().nextInt(15))
                .birthday(LocalDate.of(1990, 1, 1))
                .fightRecord(FightRecord.builder()
                        .win(num + 10)
                        .draw(num)
                        .loss(num + 1)
                        .build())
                .build();
    }

    public static Fighter createFighterWithName(String name) {
        return Fighter.builder()
                .id(1L)
                .name(name)
                .nickname(NICKNAME_PREFIX)
                .height(180)
                .weight(155.5)
                .reach(100)
                .ranking(new Random().nextInt(15))
                .birthday(LocalDate.of(1990, 1, 1))
                .fightRecord(FightRecord.builder()
                        .win(1)
                        .draw(1)
                        .loss(1)
                        .build())
                .build();
    }

    public static Fighter createFighterWithNameForPersist(String name) {
        return Fighter.builder()
                .name(name)
                .nickname(NICKNAME_PREFIX + name)
                .height(180)
                .weight(155.5)
                .reach(100)
                .ranking(1)
                .birthday(LocalDate.of(1990, 1, 1))
                .fightRecord(FightRecord.builder().win(1).draw(0).loss(0).build())
                .build();
    }

    public static Fighter createFighterWithId(Long id) {
        return Fighter.builder()
                .id(id)
                .name("NAME"+id)
                .nickname(NICKNAME_PREFIX)
                .height(180)
                .weight(155.5)
                .reach(100)
                .ranking(new Random().nextInt(15))
                .birthday(LocalDate.of(1990, 1, 1))
                .fightRecord(FightRecord.builder()
                        .win(1)
                        .draw(1)
                        .loss(1)
                        .build())
                .build();
    }

}
