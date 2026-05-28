package my.mma.api.alert.service;

import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.repository.UserPreferencesRepository;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.fcm.FcmMessageService;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.global.utils.CustomDateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static my.mma.api.global.fcm.FcmMessageService.BATCH_SIZE;

/**
 * 이번 주 경기 알림을 받는 모든 사용자들에게 (수)요일 오후 7시에 푸시 알림 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyFightEventNotificationService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final RedisUtils<CurrentEventDto> weeklyFightEventRedisUtils;
    private final FcmMessageService fcmMessageService;

    @Loggable
    @Scheduled(cron = "0 0 18 ? * WED")
    public void sendWeeklyFightEventAlert() {
        CurrentEventDto weeklyFightEvent = weeklyFightEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
        if (weeklyFightEvent == null) {
            log.warn("No weekly fight event found in Redis");
            return;
        }
        if (CustomDateUtils.isThisWeek(weeklyFightEvent.getDisplayDate())) {
            List<String> fcmTokens = userPreferencesRepository.findFcmTokensByWeeklyEventAlertEnabledIsTrue();
            for (int i = 0; i < fcmTokens.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, fcmTokens.size());
                List<String> batchTokens = fcmTokens.subList(i, end);
                MulticastMessage message = buildMulticastMessage(
                        batchTokens, weeklyFightEvent.getName(), weeklyFightEvent.getLocation());
                fcmMessageService.sendEachForMulticast(message);
            }
        }
    }

    private MulticastMessage buildMulticastMessage(List<String> tokens, String eventName, String location) {
        return MulticastMessage.builder()
                .setNotification(
                        Notification.builder()
                                .setTitle("이번 주 경기 안내")
                                .setBody("%s 경기가 %s에서 열립니다. 지금 카드를 확인해보세요!"
                                        .formatted(eventName, location))
                                .build()
                )
                .addAllTokens(tokens)
                .build();
    }
}

