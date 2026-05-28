package my.mma.api.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.entity.Alert;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.dto.FightEventDto;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.service.FightEventService;
import my.mma.api.user.dto.UserBetRecord;
import my.mma.api.user.dto.UserProfileDto;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final AlertRepository alertRepository;
    private final FighterRepository fighterRepository;
    private final FightEventRepository fightEventRepository;
    private final FightEventService eventService;

    public UserProfileDto profile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400));

        UserBetRecord userBetRecord = betRepository.getUserBetRecord(user.getId()); // nullable
        List<Alert> userAlerts = alertRepository.findByUserId(user.getId());

        List<FighterDto> alertFighters = getAlertFighters(userAlerts);
        List<FightEventDto.FighterFightEventDto> alertFightEvents = getAlertEvents(userAlerts);

        return UserProfileDto.builder()
                .userBetRecord(userBetRecord)
                .alertFighters(alertFighters)
                .alertEvents(alertFightEvents)
                .build();
    }

    private List<FighterDto> getAlertFighters(List<Alert> userAlerts) {
        List<Long> fighterIds = userAlerts.stream()
                .filter(alert -> alert.getAlertTarget().equals(AlertTarget.FIGHTER))
                .map(Alert::getTargetId)
                .toList();
        //                    fighterDto.setHeadshotUrl(s3Service.generateFighterHeadshotUrl(fighter.getName()));
        return fighterRepository.findAllById(fighterIds)
                .stream().map(FighterDto::toDto).toList();
    }

    private List<FightEventDto.FighterFightEventDto> getAlertEvents(List<Alert> userAlerts) {
        List<Long> fightEventIds = userAlerts.stream()
                .filter(alert -> alert.getAlertTarget().equals(AlertTarget.UPCOMING_EVENT))
                .map(Alert::getTargetId)
                .toList();
        return fightEventRepository.findAllById(fightEventIds)
                .stream().map(eventService::getMainCardDto).toList();
    }

}
