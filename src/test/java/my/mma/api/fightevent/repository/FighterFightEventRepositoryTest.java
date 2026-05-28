package my.mma.api.fightevent.repository;

import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.fixture.entity.fighter.FighterFixture;
import my.mma.fixture.entity.fightevent.FightEventFixture;
import my.mma.fixture.entity.fighterfightevent.FighterFightEventFixture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@DataJpaTest
@ActiveProfiles("test")
class FighterFightEventRepositoryTest {

    @Autowired
    private FighterFightEventRepository fighterFightEventRepository;
    @Autowired
    private FightEventRepository fightEventRepository;
    @Autowired
    private FighterRepository fighterRepository;

    private final String name1 = "name1";
    private final String name2 = "name2";
    private final String name3 = UUID.randomUUID().toString();
    private final int year = LocalDate.now().getYear();

    @BeforeEach
    void setup() {
        Fighter winner = fighterRepository.save(FighterFixture.createFighterWithNameForPersist(name1));
        Fighter loser = fighterRepository.save(FighterFixture.createFighterWithNameForPersist(name2));
        Fighter other1 = fighterRepository.save(FighterFixture.createFighterWithNameForPersist("other-" + UUID.randomUUID()));
        Fighter other2 = fighterRepository.save(FighterFixture.createFighterWithNameForPersist("other-" + UUID.randomUUID()));

        FightEvent fe1 = FightEventFixture.createFightEventForPersist(year);
        fe1.getFighterFightEvents().add(FighterFightEventFixture.createForPersist(fe1, other1, other2));

        FightEvent fe2 = FightEventFixture.createFightEventForPersist(year);
        fe2.getFighterFightEvents().add(FighterFightEventFixture.createForPersist(fe2, winner, other1));

        FightEvent fe3 = FightEventFixture.createFightEventForPersist(year);
        fe3.getFighterFightEvents().add(FighterFightEventFixture.createForPersist(fe3, winner, other2));

        FightEvent fe4 = FightEventFixture.createFightEventForPersist(year);
        fe4.getFighterFightEvents().add(FighterFightEventFixture.createForPersist(fe4, other1, loser));

        fightEventRepository.saveAll(List.of(fe1, fe2, fe3, fe4));
    }

    @Test
    @DisplayName("[JPQL] 입력으로 들어온 Fighter 및 년도가 포함된 모든 fighterFightEvent 목록 반환")
    void findByFighter_fromFighterAndYearFightEventRepository() {
        //when
        Fighter fighter1 = fighterRepository.findByName(name1).get();
        Fighter fighter2 = fighterRepository.findByName(name2).get();
        Fighter fighter3 = FighterFixture.createFighterWithNameForPersist(name3);
        fighterRepository.saveAndFlush(fighter3);
        List<FighterFightEvent> cardsIncludingName1 = fighterFightEventRepository.findByFighterIdAndYear(fighter1.getId(), year);
        List<FighterFightEvent> cardsIncludingName2 = fighterFightEventRepository.findByFighterIdAndYear(fighter2.getId(), year);
        List<FighterFightEvent> emptyCard = fighterFightEventRepository.findByFighterIdAndYear(fighter3.getId(), year);

        //then
        Assertions.assertThat(cardsIncludingName1.size()).isEqualTo(2);
        Assertions.assertThat(cardsIncludingName1.get(0).getWinner()).isEqualTo(fighter1);
        Assertions.assertThat(cardsIncludingName1.get(1).getWinner()).isEqualTo(fighter1);

        Assertions.assertThat(cardsIncludingName2.size()).isEqualTo(1);
        Assertions.assertThat(cardsIncludingName2.getFirst().getLoser()).isEqualTo(fighter2);

        Assertions.assertThat(emptyCard).isEmpty();
    }

}