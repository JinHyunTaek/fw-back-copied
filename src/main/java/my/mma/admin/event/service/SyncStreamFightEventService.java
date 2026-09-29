package my.mma.admin.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.event.dto.CrawledPrevEvent;
import my.mma.admin.event.dto.CrawledUpcomingEvent;
import my.mma.api.alert.service.CurrentFightEventNotificationService;
import my.mma.api.global.fcm.AdminPushNotificationService;
import my.mma.api.global.logaop.Loggable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncStreamFightEventService {

    private final FlaskEventService flaskSyncEventService;
    private final AdminSaveCurrentEventService saveStreamFightEventService;
    private final BetPointHandler betPointHandler;
    private final CurrentFightEventNotificationService currentFightEventNotificationService;
    private final AdminPushNotificationService adminPushNotificationService;

    // DB & Redis 동기화. handleBets=true면 배팅 정산 + 사용자 알림 전송
    @Loggable
    public void syncAll(boolean handleBets, Long eventId) {
        // 느린 스크래핑(upcoming/prev)은 트랜잭션 밖에서 먼저 끝내고, DB 반영만 짧은 트랜잭션(apply)에서 수행한다.
        try {
            CrawledUpcomingEvent upcoming = flaskSyncEventService.fetchUpcoming();
            Map<Long, CrawledPrevEvent> prevById = flaskSyncEventService.fetchPrevForPastEvents(upcoming);
            flaskSyncEventService.apply(upcoming, prevById);
        } catch (Exception e) {
            log.error("Error while synchronizing fight events, e=", e);
            adminPushNotificationService.sendNotificationToAdmin("Error while synchronizing fight events");
        }
        if (handleBets) {
            betPointHandler.handleUserBets(eventId);
//            currentFightEventNotificationService.sendBetSettlementNotification(eventId);
        }
        saveStreamFightEventService.syncStreamFightEvent();
        currentFightEventNotificationService.initializeTaskScheduler();
    }

    /**
     * bonus polling에서 fotn/potn 갱신 여부 확인.
     *
     * @return 하나라도 fotn/potn이 있으면 true
     */
    public boolean checkBonusReady(Long eventId) {
        return flaskSyncEventService.syncBonusDataForEvent(eventId);
    }

}