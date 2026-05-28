package my.mma.api.user.service;

import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.entity.Alert;
import my.mma.api.alert.entity.UserPreferences;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.alert.repository.UserPreferencesRepository;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.entity.BetPrediction;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.entity.UserFighterRating;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fighter.repository.UserFighterRatingRepository;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.inquiry.entity.Inquiry;
import my.mma.api.inquiry.entity.constant.InquiryCategory;
import my.mma.api.inquiry.repository.InquiryRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import my.mma.fixture.entity.fighter.FighterFixture;
import my.mma.fixture.entity.fighterfightevent.FighterFightEventFixture;
import my.mma.fixture.entity.fightevent.FightEventFixture;
import my.mma.fixture.entity.user.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(UserReferencedEntitiesDeleteService.class)
@DataJpaTest
@ActiveProfiles("test")
class UserReferencedEntitiesDeleteServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private BetCardRepository betCardRepository;
    @Autowired
    private BetRepository betRepository;
    @Autowired
    private InquiryRepository inquiryRepository;
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    @Autowired
    private UserFighterRatingRepository userFighterRatingRepository;
    @Autowired
    private FighterRepository fighterRepository;
    @Autowired
    private FightEventRepository fightEventRepository;

    @Autowired
    private UserReferencedEntitiesDeleteService userReferencedEntitiesDeleteService;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @DisplayName("사용자 탈퇴 시, 해당 사용자와 연관된 모든 데이터 삭제 (다른 사용자의 데이터는 삭제되지 않는다.)")
    @Test
    void deleteUserReferencedEntitiesIfUserWithdraw() {
        //given
        User user = UserFixture.builderForPersist()
                .nickname("nickname")
                .email("email-123")
                .point(1000)
                .earnedBetSucceedPoint(10000)
                .build();
        userRepository.save(user);

        saveAlertTargets(user);
        saveBetsWithFighterPrefix(user, "P1");
        saveInquiries(user);
        saveUserPreferences(user);
        saveUserFighterRatingsWithFighterPrefix(user, "P2");

        User anotherUser = UserFixture.builderForPersist()
                .nickname("another")
                .email("another-456")
                .build();
        userRepository.save(anotherUser);

        saveAlertTargets(anotherUser);
        saveBetsWithFighterPrefix(anotherUser, "P3");
        saveInquiries(anotherUser);
        saveUserPreferences(anotherUser);
        saveUserFighterRatingsWithFighterPrefix(anotherUser, "P4");

        //when
        userReferencedEntitiesDeleteService.deleteUserReferencedEntities(user.getId());

        // then: 탈퇴 유저 데이터 전부 삭제
        assertThat(countByUserId("alerts", user.getId())).isZero();
        assertThat(countByUserId("bet", user.getId())).isZero();
        assertThat(countByUserId("inquiry", user.getId())).isZero();
        assertThat(countByUserId("user_preferences", user.getId())).isZero();
        assertThat(countByUserId("user_fighter_rating", user.getId())).isZero();

        // then: 다른 유저 데이터 유지
        assertThat(countByUserId("alerts", anotherUser.getId())).isPositive();
        assertThat(countByUserId("bet", anotherUser.getId())).isPositive();
        assertThat(countByUserId("inquiry", anotherUser.getId())).isPositive();
        assertThat(countByUserId("user_preferences", anotherUser.getId())).isPositive();
        assertThat(countByUserId("user_fighter_rating", anotherUser.getId())).isPositive();

    }

    private int countByUserId(String table, Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE user_id = ?",
                Integer.class, userId);
    }

    private void saveUserFighterRatingsWithFighterPrefix(User user, String prefix) {
        List<UserFighterRating> ratings = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Fighter fighter = FighterFixture.createFighterWithNameForPersist(prefix + "F" + i);
            fighterRepository.save(fighter);
            UserFighterRating rating = UserFighterRating.builder()
                    .user(user)
                    .fighter(fighter)
                    .rating(i)
                    .build();
            ratings.add(rating);
        }
        userFighterRatingRepository.saveAll(ratings);
    }

    private void saveUserPreferences(User user) {
        userPreferencesRepository.save(UserPreferences.of(user));
    }

    private void saveInquiries(User user) {
        List<Inquiry> inquiries = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Inquiry inquiry = Inquiry.builder()
                    .user(user)
                    .answer("answer" + i)
                    .content("content" + i)
                    .category(InquiryCategory.FEEDBACK)
                    .build();
            inquiries.add(inquiry);
        }
        inquiryRepository.saveAll(inquiries);
    }

    private void saveAlertTargets(User user) {
        List<Alert> alerts = new ArrayList<>();
        Arrays.stream(AlertTarget.values()).forEach(
                alertTarget -> alertRepository.save(Alert.builder()
                        .user(user)
                        .alertTarget(alertTarget)
                        .targetId(1L)
                        .build())
        );
        alertRepository.saveAll(alerts);
    }

    private void saveBetsWithFighterPrefix(User user, String prefix) {
        for (int i = 0; i < 2; i++) {
            Fighter winner = fighterRepository.save(FighterFixture.createFighterWithNameForPersist(prefix + "winner-" + i));
            Fighter loser = fighterRepository.save(FighterFixture.createFighterWithNameForPersist(prefix + "loser-" + i));

            FightEvent fe = FightEventFixture.createFightEventForPersist(2000 + i);
            FighterFightEvent ffe = FighterFightEventFixture.createForPersist(fe, winner, loser);
            fe.addFighterFightEvent(ffe);
            fightEventRepository.save(fe);

            BetCard betCard = BetCard.builder()
                    .fighterFightEvent(ffe)
                    .betPrediction(BetPrediction.builder()
                            .myWinnerId(winner.getId())
                            .myLoserId(loser.getId())
                            .build())
                    .build();
            Bet bet = Bet.builder()
                    .seedPoint(100)
                    .user(user)
                    .build();
            bet.addBetCard(betCard);
            betRepository.save(bet);
        }
    }

}