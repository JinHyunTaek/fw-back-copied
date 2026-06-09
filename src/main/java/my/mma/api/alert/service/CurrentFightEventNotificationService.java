package my.mma.api.alert.service;

import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.repository.AlertRepository;
import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.fcm.AdminPushNotificationService;
import my.mma.api.global.fcm.FcmMessageService;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static my.mma.api.global.fcm.FcmMessageService.BATCH_SIZE;

/**
 * 특정 이벤트 푸시 알림 설정을 수동으로 한 사용자들에게 메인 카드 시작 1시간 전에 푸시 알림 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentFightEventNotificationService {

    private final AdminPushNotificationService adminPushNotificationService;
    private ScheduledFuture<?> sendUpcomingEventNotificationTask;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final RedisUtils<CurrentEventDto> streamFightEventRedisUtils;
    private final AlertRepository alertRepository;
    private final FcmMessageService fcmMessageService;

    @Loggable
    public void initializeTaskScheduler() {
        if (sendUpcomingEventNotificationTask != null) {
            sendUpcomingEventNotificationTask.cancel(false);
        }

        CurrentEventDto currentEvent = streamFightEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
        if (currentEvent != null) {
            CardStartDateTimeInfoDto mainCardInfo = currentEvent.getMainCardDateTimeInfo();
            if(mainCardInfo == null){
                adminPushNotificationService.sendNotificationToAdmin("이벤트 메인 카드 시작 날짜 정보가 없음");
                return;
            }
            LocalDateTime beginningDateTime = LocalDateTime.of(
                    mainCardInfo.date(), mainCardInfo.time()
            );
            Instant triggerTime = beginningDateTime
                    .minusHours(1)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toInstant();
            sendUpcomingEventNotificationTask = taskScheduler.schedule(
                    () -> sendNotificationBeforeBeginning(currentEvent),
                    triggerTime
            );
        }
    }

    @Loggable
    private void sendNotificationBeforeBeginning(CurrentEventDto currentEvent) {
        log.info("-----send notification before event starts to users-----");
        String eventName = currentEvent.getName();
        List<String> userTokens = alertRepository.findUserTokens(
                AlertTarget.UPCOMING_EVENT, currentEvent.getId());
        for (int i = 0; i < userTokens.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, userTokens.size());
            List<String> batchTokens = userTokens.subList(i, end);
            MulticastMessage message = buildMulticastMessage(batchTokens, eventName, currentEvent.getId());
            fcmMessageService.sendEachForMulticast(message);
        }
    }

    @Loggable
    public void sendBetSettlementNotification(Long eventId) {
        List<String> userTokens = alertRepository.findUserTokens(AlertTarget.UPCOMING_EVENT, eventId);
        for (int i = 0; i < userTokens.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, userTokens.size());
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle("경기 예측 점수 정산 완료")
                            .setBody("경기 예측 결과가 확정되었습니다. 확인해보세요!")
                            .build())
                    .putData("type", "BET_SETTLEMENT")
                    .putData("eventId", String.valueOf(eventId))
                    .addAllTokens(userTokens.subList(i, end))
                    .build();
            fcmMessageService.sendEachForMulticast(message);
        }
    }

    private MulticastMessage buildMulticastMessage(List<String> tokens, String eventName, long eventId) {
        return MulticastMessage.builder()
                .setNotification(
                        Notification.builder()
                                .setTitle("메인 카드 시작 알림")
                                .setBody(eventName + " 메인 카드가 곧 시작됩니다!")
                                .build()
                )
                .putData("type", "EVENT_START")
                .putData("eventId", String.valueOf(eventId))
                .addAllTokens(tokens)
                .build();
    }
}
