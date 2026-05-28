package my.mma.api.alert.service;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.entity.Alert;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.fcm.FcmMessageService;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.user.entity.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static my.mma.api.global.utils.CustomDateUtils.isThisWeek;

/**
 * User가 푸시 알림 등록한 fighter에 대하여 fcm 기반 전역 알림 전송
 * 이번 주 FightEvent DB에 저장되기 직전에 호출됨. (Notification -> Save FightEvent -> Save StreamFightEvent)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FighterNotificationService {

    private final AlertRepository alertRepository;
    private final RedisUtils<CurrentEventDto> streamFightEventRedisUtils;
    private final FcmMessageService fcmMessageService;

    //    @Async("fighter notification") // 별도의 스레드풀
//    @SchedulerLock(name = "fighterNotification")
//    @Scheduled(cron = "0 0 2 * * TUE") // 매주 월요일 오후 18시
    @Loggable
    @Scheduled(cron = "0 0 18 * * TUE")
    public void sendNotification() {
        CurrentEventDto currentEvent = streamFightEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
        if (currentEvent != null) {
            LocalDate date = currentEvent.getDisplayDate();
            if (isThisWeek(date)) {
                List<Message> messages = new ArrayList<>();
                String eventName = currentEvent.getName();

                Map<Long, String> fighterNameMap = getFighterNameMapFromCurrentEvent(currentEvent);

                Set<Long> fighterIds = currentEvent.getFighterFightEvents().stream()
                        .flatMap(ffe -> Stream.of(ffe.getWinner().getId(), ffe.getLoser().getId())).collect(Collectors.toSet());

                List<Alert> alerts = alertRepository.findByAlertTargetAndTargetIdIn(AlertTarget.FIGHTER, fighterIds);
                Map<User, List<Alert>> userAlerts = alerts.stream()
                        .collect(Collectors.groupingBy(Alert::getUser));
                // user : List<Alert> (target: fighter)
                for (var userAlertsEntry : userAlerts.entrySet()) {
                    User user = userAlertsEntry.getKey();
                    String fcmToken = user.getFcmToken();
                    if (fcmToken == null || fcmToken.isBlank()) {
                        continue;
                    }
                    // event에 포함된 fighters 중 user가 알림 설정한 fighters 로 filtering
                    List<String> userAlertedFighterNames = userAlertsEntry.getValue().stream()
                            .map(alert -> fighterNameMap.get(alert.getTargetId()))
                            .toList();
                    if (!userAlertedFighterNames.isEmpty()) {
                        log.info("send notification to {}, fighters = {}", user.getNickname(), userAlertedFighterNames);
                        messages.add(buildFcmMessage(userAlertedFighterNames, eventName, user.getFcmToken()));
                    }
                }
                fcmMessageService.sendEach(messages);
            }
        }
    }

    private static Map<Long, String> getFighterNameMapFromCurrentEvent(CurrentEventDto streamFightEvent) {
        return streamFightEvent.getFighterFightEvents().stream()
                .flatMap(ffe -> Stream.of(
                        ffe.getWinner(),
                        ffe.getLoser()
                ))
                .collect(Collectors.toMap(
                        FighterDto::getId,
                        FighterDto::getName,
                        (a, b) -> a
                ));
    }

    private static Message buildFcmMessage(List<String> userAlertedFighterNames, String eventName, String fcmToken) {
        StringBuilder body = new StringBuilder(userAlertedFighterNames.getFirst());
        if (userAlertedFighterNames.size() > 1)
            body.append(" 외 ").append(userAlertedFighterNames.size() - 1).append("명의 선수");
        body.append("의 경기가 ").append(eventName).append("에 잡혔습니다!");

        return Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle("경기 알림")
                        .setBody(body.toString())
                        .build())
                .build();
    }
}
