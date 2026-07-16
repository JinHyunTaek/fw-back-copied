package my.mma.api.fightevent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fightevent.dto.CurrentEventDto.FighterFightEventCardFighterDto;
import my.mma.api.fightevent.dto.FightEventDto;
import my.mma.api.fightevent.dto.FightEventDto.FighterFightEventDto;
import my.mma.api.fightevent.dto.FighterFightEventCardDetailDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static my.mma.api.exception.ErrorCode.BAD_REQUEST_400;
import static my.mma.api.exception.ErrorCode.NO_SUCH_USER_FOUND_400;

@Transactional(readOnly = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class FightEventService {

    private final FightEventRepository fightEventRepository;
    private final FighterFightEventRepository fighterFightEventRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final S3ImgService s3Service;

    public List<Integer> getEventDaysFromYearMonth(int year, int month){
        YearMonth ym = YearMonth.of(year, month);
        return fightEventRepository.findDisplayDatesByYearMonth(ym.atDay(1),ym.atEndOfMonth())
                .stream()
                .map(LocalDate::getDayOfMonth)
                .toList();
    }

    public List<FightEventDto> getSchedule(LocalDate date, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NO_SUCH_USER_FOUND_400));
        List<FightEvent> schedules = fightEventRepository.findByDisplayDateWithFightsNotCanceled(date);
        return schedules.stream().map(
                fightEvent -> {
                    FightEventDto fightEventDto = FightEventDto.toDto(fightEvent);
                    if (fightEventDto.isUpcoming()) {
                        fightEventDto.setAlert(alertRepository.existsByUserAndAlertTargetAndTargetId(user, AlertTarget.UPCOMING_EVENT, fightEvent.getId()));
                    }
                    fightEventDto.getFighterFightEvents().forEach(
                            fighterFightEventDto -> {
                                fighterFightEventDto.getWinner().setHeadshotUrl(s3Service.generateFighterHeadshotUrlOrNull(
                                        fighterFightEventDto.getWinner().getName()
                                ));
                                fighterFightEventDto.getLoser().setHeadshotUrl(s3Service.generateFighterHeadshotUrlOrNull(
                                        fighterFightEventDto.getLoser().getName()
                                ));
                            }
                    );
                    return fightEventDto;
                }
        ).toList();
    }

    public Page<FighterFightEventDto> search(String name, Pageable pageable) {
        Optional<Page<FightEvent>> events = fightEventRepository.findByNameContainingIgnoreCase(name, pageable);
        return events.map(
                page -> page.map(
                        this::getMainCardDto
                )
        ).orElse(null);
    }

    public FighterFightEventDto getMainCardDto(FightEvent fightEvent) {
        FighterFightEvent mainFight = fightEvent.getFighterFightEvents().getFirst();
        FighterFightEventDto mainCardDto = FighterFightEventDto.toDto(mainFight);
        mainCardDto.getWinner().setHeadshotUrl(s3Service.generateFighterHeadshotUrlOrNull(mainFight.getWinner().getName()));
        mainCardDto.getLoser().setHeadshotUrl(s3Service.generateFighterHeadshotUrlOrNull(mainFight.getLoser().getName()));
        return mainCardDto;
    }

    public FighterFightEventCardDetailDto cardDetail(Long ffeId) {
        FighterFightEvent ffe = fighterFightEventRepository.findById(ffeId)
                .orElseThrow(() -> new CustomException(BAD_REQUEST_400));
        Fighter winner = ffe.getWinner();
        Fighter loser = ffe.getLoser();
        FighterFightEventCardFighterDto winnerCardDto = FighterFightEventCardFighterDto.of(winner);
        FighterFightEventCardFighterDto loserCardDto = FighterFightEventCardFighterDto.of(loser);
        winnerCardDto.setBodyUrl(s3Service.generateFighterBodyUrlOrNull(winner.getName()));
        loserCardDto.setBodyUrl(s3Service.generateFighterBodyUrlOrNull(loser.getName()));
        return new FighterFightEventCardDetailDto(winnerCardDto, loserCardDto, ffe.getFightWeight().getDisplayName());
    }
}
