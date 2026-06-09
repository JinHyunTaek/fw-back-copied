package my.mma.admin.event.service;

import lombok.RequiredArgsConstructor;
import my.mma.api.alert.service.CurrentFightEventNotificationService;
import my.mma.api.global.logaop.Loggable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyncStreamFightEventService {

    private final FlaskEventService flaskSyncEventService;
    private final AdminSaveCurrentEventService saveStreamFightEventService;
    private final BetPointHandler betPointHandler;
    private final CurrentFightEventNotificationService currentFightEventNotificationService;

    // DB & Redis 동기화. handleBets=true면 배팅 정산 + 사용자 알림 전송
    @Loggable
    public void syncAll(boolean handleBets, Long eventId) {
        flaskSyncEventService.syncFightEvents();
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