package my.mma.api.user.service;

import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.entity.Alert;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.dto.FightEventDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.service.FightEventService;
import my.mma.api.user.dto.UserBetRecord;
import my.mma.api.user.dto.UserProfileDto;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static my.mma.api.fightevent.dto.FightEventDto.FighterFightEventDto.toDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BetRepository betRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private FighterRepository fighterRepository;
    @Mock
    private FightEventRepository fightEventRepository;
//    @Mock
//    private S3ImgService s3ImgService;
    @Mock
    private FightEventService eventService;

    @InjectMocks
    private UserProfileService userProfileService;

    private User user;

    private final long alertFighterId1 = 2L;
    private final long alertFighterId2 = 3L;

    private final long alertEventId = 2L;
    private final String alertEventName = "event-name2";

    private final String fighterNamePrefix = "name";
    private final String fighterNicknamePrefix = "nickname";

    @BeforeEach
    void setupUserProfile() {
        setupUser();
    }

    @Test
    @DisplayName("사용자 상세 정보 반환")
    void returnUserProfileDto_givenUserEmail() {
        //given
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(betRepository.getUserBetRecord(user.getId())).thenReturn(getUserBetRecord());
        when(alertRepository.findByUserId(user.getId())).thenReturn(getAlerts());
        when(fighterRepository.findAllById(Mockito.any())).thenReturn(getAlertFighters());
        List<FightEvent> alertFightEvents = getAlertCards();
        when(fightEventRepository.findAllById(Mockito.any())).thenReturn(alertFightEvents);

        String s3ImgUrl = "generated-s3-img-url";
//        when(s3ImgService.generateFighterHeadshotUrl(Mockito.any())).thenReturn(s3ImgUrl);
        FightEventDto.FighterFightEventDto alertMainCardsDto = toDto(alertFightEvents.getFirst().getFighterFightEvents().getFirst());

        when(eventService.getMainCardDto(alertFightEvents.getFirst()))
                .thenReturn(alertMainCardsDto);

        //when
        UserProfileDto profile = userProfileService.profile(user.getEmail());

        //then
        assertThat(profile.userBetRecord()).usingRecursiveComparison().isEqualTo(getUserBetRecord());

        assertThat(profile.alertEvents().size()).isEqualTo(1);
        assertThat(profile.alertEvents().getFirst()).usingRecursiveComparison().isEqualTo(alertMainCardsDto);

        assertThat(profile.alertFighters().size()).isEqualTo(2);
        assertThat(profile.alertFighters().getFirst().getId()).isEqualTo(alertFighterId1);
        assertThat(profile.alertFighters().get(0).getName()).isEqualTo(fighterNamePrefix+alertFighterId1);
//        assertThat(profile.alertFighters().get(0).getHeadshotUrl()).isEqualTo(s3ImgUrl);
        assertThat(profile.alertFighters().get(1).getId()).isEqualTo(alertFighterId2);
        assertThat(profile.alertFighters().get(1).getName()).isEqualTo(fighterNamePrefix+alertFighterId2);
//        assertThat(profile.alertFighters().get(1).getHeadshotUrl()).isEqualTo(s3ImgUrl);
    }

    private void setupUser() {
        user = User.builder()
                .id(1L)
                .nickname("nickname123")
                .email("email123@google.com")
                .point(0)
                .role("ROLE_USER")
                .password("pwd123")
                .build();
    }

    private UserBetRecord getUserBetRecord() {
        return new UserBetRecord() {
            @Override
            public int getWin() {
                return 11;
            }

            @Override
            public int getLoss() {
                return 2;
            }

            @Override
            public int getNoContest() {
                return 0;
            }
        };
    }

    private List<Alert> getAlerts() {
        Alert alert = Alert.builder()
                .id(1L)
                .user(user)
                .alertTarget(AlertTarget.FIGHTER)
                .targetId(alertFighterId1)
                .build();
        List<Alert> alerts = new ArrayList<>();
        alerts.add(alert);
        return alerts;
    }

    private List<Fighter> getAlertFighters() {
        Fighter fighter1 = Fighter.builder()
                .id(alertFighterId1)
                .name(fighterNamePrefix +alertFighterId1)
                .nickname(fighterNicknamePrefix +alertFighterId1)
                .reach(0)
                .height(0)
                .ranking(1)
                .birthday(LocalDate.now())
                .build();
        Fighter fighter2 = Fighter.builder()
                .id(alertFighterId2)
                .name(fighterNamePrefix +alertFighterId2)
                .nickname(fighterNicknamePrefix +alertFighterId2)
                .reach(0)
                .height(0)
                .ranking(1)
                .birthday(LocalDate.now())
                .build();
        List<Fighter> fighters = new ArrayList<>();
        fighters.add(fighter1);
        fighters.add(fighter2);
        return fighters;
    }

    private List<FightEvent> getAlertCards() {
        List<FightEvent> mainCards = new ArrayList<>();
        FightEvent fightEvent = FightEvent.builder()
                .id(alertEventId)
                .location("location1")
                .eventDate(LocalDate.now())
                .completed(false)
                .name(alertEventName)
                .build();
        FighterFightEvent fighterFightEvent = FighterFightEvent.builder()
                .fightWeight(FightWeight.FEATHERWEIGHT)
                .winner(getAlertFighters().get(0))
                .loser(getAlertFighters().get(1))
                .fightEvent(fightEvent)
                .title(false)
                .build();
        fightEvent.getFighterFightEvents().add(fighterFightEvent);
        mainCards.add(fightEvent);
        return mainCards;
    }
}