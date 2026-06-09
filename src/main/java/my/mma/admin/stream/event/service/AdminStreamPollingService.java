package my.mma.admin.stream.event.service;

import lombok.extern.slf4j.Slf4j;
import my.mma.admin.event.service.SyncStreamFightEventService;
import my.mma.admin.stream.event.dto.AdminStreamFightEventDto;
import my.mma.admin.stream.event.dto.AdminStreamFightEventDto.AdminStreamFighterFightEventDto;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.CurrentFighterFightEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.FighterFightEventCardFighterDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.store.CurrentEventStore;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.stream.handler.GlobalWebSocketHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static my.mma.api.fightevent.dto.StreamFighterFightEventStatus.*;
import static my.mma.api.global.utils.CustomDateUtils.isThisWeek;

/**
 * 스케줄러가 백그라운드 스레드 풀 내 워커 스레드에서 작업을 실행
 * 즉, 스케줄링된 메서드는 이미 별도의 스레드에서 실행됨 (메인 스레드와 분리)
 * 따라서 스케줄링된 메서드 내 작업은 기본적으로 비동기적임
 */

@Service
@Slf4j
public class AdminStreamPollingService {

    @Value("${flask.uri}")
    private String flaskURI;
    // requestStreamFightEventTask를 스케줄러로 등록하는 스케줄러
    private ScheduledFuture<?> prePollingTask;
    private ScheduledFuture<?> pollingStreamFightEventTask;
    private ScheduledFuture<?> bonusPollingTask;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final FightEventRepository fightEventRepository;
    private final RedisUtils<CurrentEventDto> streamFightEventRedisUtils;
    private final CurrentEventStore currentEventStore;
    private final RestTemplate restTemplate;
    private final GlobalWebSocketHandler socketHandler;
    private final SyncStreamFightEventService syncStreamFightEventService;

    public AdminStreamPollingService(ThreadPoolTaskScheduler taskScheduler, FightEventRepository fightEventRepository,
                                     RedisUtils<CurrentEventDto> streamFightEventRedisUtils,
                                     CurrentEventStore currentEventStore,
                                     @Qualifier("flaskRestTemplate") RestTemplate restTemplate,
                                     GlobalWebSocketHandler socketHandler,
                                     SyncStreamFightEventService syncStreamFightEventService) {
        this.taskScheduler = taskScheduler;
        this.fightEventRepository = fightEventRepository;
        this.streamFightEventRedisUtils = streamFightEventRedisUtils;
        this.currentEventStore = currentEventStore;
        this.restTemplate = restTemplate;
        this.socketHandler = socketHandler;
        this.syncStreamFightEventService = syncStreamFightEventService;
    }

    // 매일 새벽 3시 이벤트 데이터 갱신 (폴링 중에는 스킵) * betpointhandler 실행 전에 streamfightevent 정보 바뀌면 안 됨.
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Seoul")
    public void scheduleDailySync() {
        if (!isPolling()) {
            syncAllAndSchedule();
        }
    }

    @Loggable
    public void scheduleAutoStart() {
        if (isPolling()) return;
        if (prePollingTask != null && !prePollingTask.isDone()) {
            prePollingTask.cancel(false);
        }
        CurrentEventDto currentEvent = streamFightEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
        if(currentEvent != null) {
            CardStartDateTimeInfoDto cardStartDateTimeInfoDto = currentEvent.getEarlyCardDateTimeInfo() == null ?
                    currentEvent.getPrelimCardDateTimeInfo() : currentEvent.getEarlyCardDateTimeInfo();
            LocalDateTime eventStartDateTime = LocalDateTime.of(
                    cardStartDateTimeInfoDto.date(), cardStartDateTimeInfoDto.time()
            );
            if (isThisWeek(currentEvent.getDisplayDate()) &&
                    LocalDateTime.now().isBefore(eventStartDateTime.minusMinutes(70))) {
                Instant triggerTime = eventStartDateTime
                        .minusHours(1)
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .toInstant();
                Duration period = Duration.ofMinutes(3);
                // 폴링 시작 전 1회 sync 후 polling 등록
                prePollingTask = taskScheduler.schedule(() -> {
                    syncStreamFightEventService.syncAll(false, null);
                    pollingStreamFightEventTask = taskScheduler.scheduleAtFixedRate(
                            this::requestStreamFightEventDto,
                            Instant.now(),
                            period
                    );
                }, triggerTime);
            }
        }
    }

    private boolean isPolling() {
        return (pollingStreamFightEventTask != null && !pollingStreamFightEventTask.isDone())
                || (bonusPollingTask != null && !bonusPollingTask.isDone());
    }

    @Loggable
    private void stopPolling() {
        if (prePollingTask != null) {
            prePollingTask.cancel(false);
        }
        if (pollingStreamFightEventTask != null) {
            pollingStreamFightEventTask.cancel(false);
        }
    }

    private void scheduleBonusPolling(Long eventId) {
        AtomicInteger attempt = new AtomicInteger(0);
        bonusPollingTask = taskScheduler.scheduleAtFixedRate(() -> {
            try {
                boolean bonusReady = syncStreamFightEventService.checkBonusReady(eventId);
                int count = attempt.incrementAndGet();
                log.info("bonus polling attempt={}, bonusReady={}", count, bonusReady);
                if (bonusReady || count >= 12) {
                    bonusPollingTask.cancel(false);
                    if (!bonusReady) log.warn("Bonus polling timed out (6h). fotn/potn 미확정 배팅은 succeed=null 처리.");
                    syncStreamFightEventService.syncAll(true, eventId);
                }
            } catch (Exception e) {
                log.error("Error in bonus polling", e);
            }
        }, Duration.ofMinutes(30));
    }

    private void requestStreamFightEventDto() {
        log.info("-----request stream fight event to FLASK-----");

        // 1) 락 밖: 이벤트 이름으로 Flask 호출 (느린 외부 I/O는 락 밖에서 수행)
        CurrentEventDto current = streamFightEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
        if (current == null) {
            log.info("no current event in redis");
            return;
        }
        AdminStreamFightEventDto flaskResponse = restTemplate.getForObject(flaskURI + "/stream/event?eventName="
                + current.getName(), AdminStreamFightEventDto.class);
        if (flaskResponse == null) {
            log.info("event not started yet");
            return;
        }

        // 2) 락 안: 상태/결과 read-modify-write 전체를 하나의 임계구역에서 원자적으로 처리
        AtomicBoolean completed = new AtomicBoolean(false);
        currentEventStore.mutate(event -> {
            List<CurrentFighterFightEventDto> redisCards = event.getFighterFightEvents();
            boolean changed = false;

            // 첫 번째 카드 NOW 설정 (기존 setRedisFightEventStartIfNotStarted 인라인)
            if (redisCards.getLast().getStatus().equals(UPCOMING)) {
                log.info("첫 번째 카드 NOW 상태로 설정 완료");
                redisCards.getLast().setStatus(NOW);
                changed = true;
            }

            boolean isSingleCardUpdated = false;
            for (int i = redisCards.size() - 1; i >= 0; i--) {
                CurrentFighterFightEventDto redisCard = redisCards.get(i);
                if (isSingleCardUpdated) {
                    redisCard.setStatus(NOW);
                    break;
                }
                if (redisCard.getResult() == null)
                    for (AdminStreamFighterFightEventDto crawledCard : flaskResponse.getStreamFighterFightEvents()) {
                        /*
                         * 현재 경기 결과 업데이트 및 다음 경기를 현재경기로 설정하는 조건
                         * 1. 현재 카드(Redis에 저장된)경기의 상태 = NOW 2. 크롤링한 카드의 경기 결과 존재 3. 이름 비교
                         * 만약, 레디스에 저장된 선수 이름 순서가 크롤링으로 가져온 승자, 패자 이름순과 다를 때 -> SWAP
                         */
                        if (redisCard.getStatus().equals(NOW) && crawledCard.getResult() != null &&
                                compareNames(redisCard, crawledCard)) {
                            redisCard.setResult(crawledCard.getResult());
                            redisCard.setStatus(PREVIOUS);
                            if (!redisCard.getWinner().getName().equals(crawledCard.getWinnerName())) {
                                FighterFightEventCardFighterDto originalWinner = redisCard.getWinner();
                                redisCard.setWinner(redisCard.getLoser());
                                redisCard.setLoser(originalWinner);
                            }
                            isSingleCardUpdated = true;
                            break;
                        }
                    }
                if (redisCard.getStatus().equals(NOW) && !crawledCardsContainsRedisCard(flaskResponse.getStreamFighterFightEvents(), redisCard)) {
                    redisCard.setStatus(CANCELED);
                    isSingleCardUpdated = true;
                }
            }

            if (isSingleCardUpdated) {
                changed = true;
                log.info("=====updated stream fight event=====");
                // 이벤트의 모든 카드에 대한 경기가 끝난 케이스 → 락 밖에서 처리하도록 플래그만 세팅
                if (redisCards.stream().allMatch(card -> card.getResult() != null)) {
                    completed.set(true);
                }
            }
            return changed ? event : null;
        });

        // 3) 락 밖: DB 업데이트 / 폴링 제어 (느린 작업은 락 밖에서)
        if (completed.get()) {
            stopPolling();
            fightEventRepository.findById(current.getId()).ifPresentOrElse(
                    FightEvent::updateFightEventToCompleted,
                    () -> {
                        throw new CustomException(ErrorCode.SERVER_ERROR_500);
                    }
            );
            scheduleBonusPolling(current.getId());
        }

        // voteRate의 실시간 변동 때문에 이벤트 업데이트 여부와 상관 없이 주기적으로 broadCast 해야 됨 (최신 상태 재조회)
        socketHandler.broadcastFightEvent(streamFightEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey()));
    }

    @Loggable
    public void syncAllAndSchedule() {
        syncStreamFightEventService.syncAll(false, null);
        scheduleAutoStart();
    }

    public String getPrePollingScheduledTime() {
        if (prePollingTask == null || prePollingTask.isDone()) {
            return null;
        }
        long delaySeconds = prePollingTask.getDelay(TimeUnit.SECONDS);
        ZonedDateTime scheduledTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).plusSeconds(delaySeconds);
        return scheduledTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    public boolean crawledCardsContainsRedisCard(
            List<AdminStreamFighterFightEventDto> crawledCards,
            CurrentFighterFightEventDto redisCard) {
        for (AdminStreamFighterFightEventDto crawledCard : crawledCards) {
            if (compareNames(redisCard, crawledCard))
                return true;
        }
        return false;
    }

    boolean compareNames(CurrentFighterFightEventDto redisCard, AdminStreamFighterFightEventDto crawledCard) {
        return crawledCard.getWinnerName().equals(redisCard.getWinner().getName()) ||
                crawledCard.getWinnerName().equals(redisCard.getLoser().getName()) ||
                crawledCard.getLoserName().equals(redisCard.getLoser().getName()) ||
                crawledCard.getLoserName().equals(redisCard.getWinner().getName());
    }

}
