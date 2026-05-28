package my.mma.api.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.dto.UpdatePreferenceRequest;
import my.mma.api.alert.dto.UserPreferencesDto;
import my.mma.api.alert.dto.UpdateAlertRequest;
import my.mma.api.alert.repository.UserPreferencesRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.alert.entity.Alert;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.alert.entity.UserPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    // setting이 아닌 event/fighter 화면에서 특정 객체 alert(fighter는 좋아요) 요청
    @Transactional
    public void updateSingleAlert(String email, UpdateAlertRequest request) {
        User user = getUser(email);
        if (request.on()) {
            UserPreferences userPreferences = extractUserPreference(user.getId());
            if (request.alertTarget().equals(AlertTarget.FIGHTER) &&
                    !userPreferences.isFighterAlertEnabled()) {
                userPreferences.toggleFighterAlertEnabled();
            } else if (request.alertTarget().equals(AlertTarget.UPCOMING_EVENT) &&
                    !userPreferences.isUpcomingEventAlertEnabled()) {
                userPreferences.toggleUpcomingEventAlertEnabled();
            }
            Alert alert = request.toEntity(user);
            alertRepository.save(alert);
        } else {
            alertRepository.deleteByUserAndAlertTargetAndTargetId(user, request.alertTarget(), request.targetId());
        }
    }

    public UserPreferencesDto getUserPreferences(String email) {
        User user = getUser(email);
        UserPreferences ups = extractUserPreference(user.getId());
        return new UserPreferencesDto(
                ups.isFighterAlertEnabled(),
                ups.isUpcomingEventAlertEnabled(),
                ups.isWeeklyEventAlertEnabled());
    }

    // 설정 화면에서 모든 alertTarget on/off
    @Transactional
    public void updateAllPreferences(String email, boolean isOn) {
        User user = getUser(email);
        UserPreferences userPreferences = extractUserPreference(user.getId());
        userPreferences.toggleAll(isOn);
        if (!isOn)
            alertRepository.deleteAllByUserId(user.getId());
    }

    // 설정 화면에서 특정 alertTarget on/off
    @Transactional
    public void updateSinglePreference(String email, UpdatePreferenceRequest request) {
        User user = getUser(email);
        UserPreferences userPreferences = extractUserPreference(user.getId());
        if (request.alertTarget() != null) {
            if (request.alertTarget().equals(AlertTarget.FIGHTER))
                userPreferences.toggleFighterAlertEnabled();
            else if (request.alertTarget().equals(AlertTarget.UPCOMING_EVENT))
                userPreferences.toggleUpcomingEventAlertEnabled();
        } else if (request.isWeeklyEvent())
            userPreferences.toggleWeeklyEventAlertEnabled();
        else
            throw new CustomException(ErrorCode.BAD_REQUEST_400);
        if (!request.on() && !request.isWeeklyEvent())
            alertRepository.deleteAllByUserIdAndAlertTarget(user.getId(), request.alertTarget());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400)
        );
    }

    public UserPreferences extractUserPreference(Long userId) {
        return userPreferencesRepository.findByUserId(userId);
    }

}
