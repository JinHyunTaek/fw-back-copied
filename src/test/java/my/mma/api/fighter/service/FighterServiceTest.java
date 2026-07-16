package my.mma.api.fighter.service;

import my.mma.api.fighter.repository.UserFighterRatingRepository;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.fighter.dto.FighterDetailDto;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import my.mma.fixture.entity.user.UserFixture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.ceil;
import static my.mma.api.exception.ErrorCode.NO_SUCH_FIGHTER_CONFIGURED_400;
import static my.mma.fixture.entity.fighter.FighterFixture.*;
import static my.mma.fixture.entity.fightevent.FightEventFixture.createUpcomingFightEventWithId;
import static my.mma.api.alert.constant.AlertTarget.FIGHTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FighterServiceTest {

    @Mock
    private FighterRepository fighterRepository;
    @Mock
    private FighterFightEventRepository fighterFightEventRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserFighterRatingRepository userFighterRatingRepository;
    @Mock
    private S3ImgService s3Service;

    @InjectMocks
    private FighterService fighterService;

    @DisplayName("파이터 ID와 사용자의 알림 설정 정보를 기반으로 상세 DTO를 반환한다")
    @Test
    void returnFighterDetailDto_givenValidEmailAndFighterId() {
        //given
        String email = "myEmail123@google.com";
        long fighterId = 5L;
        Fighter fighter = createFighterWithNumber((int) fighterId);
        List<FighterFightEvent> ffes = getFighterFightEvents();
        User user = UserFixture.builder().build();
        boolean isAlertExists = true;
        String imgUrl = "img-url";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(alertRepository.existsByUserAndAlertTargetAndTargetId(user, FIGHTER, fighterId)).thenReturn(isAlertExists);
        when(fighterRepository.findById(fighterId)).thenReturn(Optional.of(fighter));
        when(fighterFightEventRepository.findByFighterIdAndYear(fighter.getId(), LocalDate.now().getYear())).thenReturn(ffes);
//        when(s3Service.generateFighterBodyUrl(anyString())).thenReturn(imgUrl);

        //when
        FighterDetailDto fighterDetailDto = fighterService.detail(email, fighterId);

        //then
        assertThat(fighterDetailDto.getId()).isEqualTo(fighter.getId());
        assertThat(fighterDetailDto.getName()).contains(NAME_PREFIX);
        assertThat(fighterDetailDto.getNickname()).contains(NICKNAME_PREFIX);
        assertThat(fighterDetailDto.getHeight()).isEqualTo(fighter.getHeight());
        assertThat(fighterDetailDto.getWeight()).isEqualTo(fighter.getWeight());
        assertThat(fighterDetailDto.getBirthday()).isEqualTo(fighter.getBirthday());
        assertThat(fighterDetailDto.getReach()).isEqualTo(fighter.getReach());
        assertThat(fighterDetailDto.getRanking()).isEqualTo(fighter.getRanking());
        assertThat(fighterDetailDto.getRecord()).isEqualTo(fighter.getFightRecord());
//        assertThat(fighterDetailDto.getBodyUrl()).isNotNull();
        assertThat(fighterDetailDto.isAlert()).isEqualTo(isAlertExists);
        assertThat(fighterDetailDto.getFighterFightEvents().size()).isEqualTo(ffes.size());
    }

    @DisplayName("입력된 파이터 ID가 DB에 없다면, 400 예외를 던진다")
    @Test
    void throwException400_whenFighterIdNotFound() {
        //given
        String email = "myEmail123@google.com";
        long fighterId = 5L;
        when(fighterRepository.findById(fighterId)).thenReturn(Optional.empty());

        //when && then
        Assertions.assertThatThrownBy(() -> fighterService.detail(email, fighterId))
                .isInstanceOf(CustomException.class)
                .hasMessage(NO_SUCH_FIGHTER_CONFIGURED_400.name());
    }

    @DisplayName("검색 키워드(파이터명)와 pageable을 기준으로 파이터 목록을 반환한다")
    @Test
    void returnFighterDtoPage_givenNameSearched() {
        //given
        String imgUrl = "img-url";
        int pageSize = 10;
        int fighterCount = 20;
        Pageable pageable = PageRequest.of(0, pageSize);
        Page<Fighter> fighterPage = getFightersPage(fighterCount, pageable);
        when(fighterRepository.searchByNameOrKoreanName(anyString(), eq(pageable))).thenReturn(fighterPage);
//        when(s3Service.generateFighterHeadshotUrl(anyString())).thenReturn(imgUrl);

        //when
        Page<FighterDto> fighterDtoPage = fighterService.search("", pageable);

        //then
        List<Fighter> pagedFighters = fighterPage.toList();
        List<FighterDto> fighterDtos = fighterDtoPage.toList();
        for (int i = 0; i < fighterDtos.size(); i++) {
            assertThat(fighterDtos.get(i).getId()).isEqualTo(pagedFighters.get(i).getId());
            assertThat(fighterDtos.get(i).getName()).isEqualTo(pagedFighters.get(i).getName());
            assertThat(fighterDtos.get(i).getRecord()).isEqualTo(pagedFighters.get(i).getFightRecord());
//            assertThat(fighterDtos.get(i).getHeadshotUrl()).isEqualTo(imgUrl);
        }
        assertThat(fighterDtoPage.getSize()).isEqualTo(pageSize);
        assertThat(fighterDtoPage.getTotalPages()).isEqualTo((int) ceil((double) fighterCount / pageSize));
    }

    private static List<FighterFightEvent> getFighterFightEvents() {
        List<FightEvent> fightEvents = new ArrayList<>();
        fightEvents.add(createUpcomingFightEventWithId(1));
        fightEvents.add(createUpcomingFightEventWithId(2));
        return fightEvents.stream().flatMap(fightEvent -> fightEvent.getFighterFightEvents().stream()).toList();
    }

    private Page<Fighter> getFightersPage(int range, Pageable pageable) {
        List<Fighter> fighters = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            Fighter fighter = createFighterWithNumber(i);
            fighters.add(fighter);
        }
        return new PageImpl<>(fighters, pageable, fighters.size());
    }

}