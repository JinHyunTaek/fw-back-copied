package my.mma.api.fighter.repository;

import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class FighterRepositoryTest {

    @Autowired
    private FighterRepository fighterRepository;

    private final int totalFighterCount = 20;
    private final String namePrefix = "name";

    @BeforeEach
    void setup() {
        // given
        for (int i = 0; i < totalFighterCount; i++) {
            Fighter fighter = Fighter.builder()
                    .name(namePrefix + i)
                    .nickname("nickname" + i)
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
            fighterRepository.save(fighter);
        }
    }

}
