package my.mma.admin.event.service;

import lombok.extern.slf4j.Slf4j;
import my.mma.admin.event.dto.CrawledFightCard;
import my.mma.admin.event.dto.CrawledPrevEvent;
import my.mma.admin.event.dto.CrawledUpcomingEvent;
import my.mma.admin.event.dto.CrawledUpcomingEvent.EventCrawlerDto;
import my.mma.api.exception.CustomException;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.utils.FightEventUtils;
import my.mma.api.global.fcm.AdminPushNotificationService;
import my.mma.api.global.logaop.Loggable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static my.mma.admin.event.dto.CrawledUpcomingEvent.FighterCrawlerDto;
import static my.mma.api.exception.ErrorCode.SERVER_ERROR_500;
import static my.mma.api.fighter.entity.FightRecord.toFightRecord;

@Component
@Slf4j
public class FlaskEventService {

    @Value("${flask.uri}")
    private String flaskURI;

    private final FighterRepository fighterRepository;
    private final FightEventRepository fightEventRepository;
    private final RestTemplate restTemplate;
    private final AdminPushNotificationService adminPushNotificationService;

    public FlaskEventService(
            FighterRepository fighterRepository,
            FightEventRepository fightEventRepository,
            @Qualifier("flaskRestTemplate") RestTemplate restTemplate,
            AdminPushNotificationService adminPushNotificationService) {
        this.fighterRepository = fighterRepository;
        this.fightEventRepository = fightEventRepository;
        this.restTemplate = restTemplate;
        this.adminPushNotificationService = adminPushNotificationService;
    }

    /**
     * flask api: 차후 경기들 및 해당 경기에 참여하는 파이터 정보 모두 반환
     */
    @Loggable
    @Transactional
    public void syncFightEvents() {
        try {
            processFetchedEventData(handleGetRequest(flaskURI + "/upcoming_event", CrawledUpcomingEvent.class));
        } catch (Exception e) {
            log.error("Error while synchronizing fight events, e=", e);
            adminPushNotificationService.sendNotificationToAdmin("Error while synchronizing fight events");
        }
    }

    private void processFetchedEventData(CrawledUpcomingEvent dto) {
        List<FightEvent> existingUpcomingEvents = fightEventRepository.findByCompletedIsFalse();
        // DB에 존재하는 upcoming Events
        //
        List<FightEvent> crawledUpcomingEvents = dto.events().stream()
                .map(EventCrawlerDto::toEntityForEventName)
                .toList();
        // fighter 부터 삽입
        Map<String, Fighter> crawledFighterNameMap = fighterRepository.findAllByNameIn(
                        dto.fighters().stream().map(FighterCrawlerDto::name).toList())
                .stream().collect(Collectors.toMap(Fighter::getName, f -> f));
        saveOrUpdateFighters(dto.fighters(), crawledFighterNameMap);
        // upcoming -> past 상태가 된 이벤트를 업데이트
        markPastEvents(existingUpcomingEvents, crawledUpcomingEvents);
        // 기존 db에 없던 새로 생긴 upcoming event 삽입
        saveNewUpcomingEvents(dto.events(), existingUpcomingEvents, crawledFighterNameMap);
    }

    private void markPastEvents(List<FightEvent> existingUpcomingEvents, List<FightEvent> crawledUpcomingEvents) {
        /** DB에 존재하는 upcoming events, 새로 불러온 upcoming events 비교
         * 새로 불러온 upcoming event list에 DB에 존재하는 upcoming event가 포함되지 않으면,
         * DB의 upcoming event를 complete 상태로 update (+해당 이벤트에 포함된 fighter 전적 업데이트)
         */
        existingUpcomingEvents.stream()
                .filter(e -> crawledUpcomingEvents.stream().noneMatch(crawledEvent ->
                        crawledEvent.getEventDate().equals(e.getEventDate())))
                .forEach(this::markEventAsCompleted);
    }

    private void markEventAsCompleted(FightEvent eventToMarkComplete) {
        LocalDate eventDate = eventToMarkComplete.getEventDate();
        String eventName = eventToMarkComplete.getName();
        log.info("mark event as completed, eventName={}", eventName);
        String url = UriComponentsBuilder.fromHttpUrl(flaskURI + "/prev_event")
                .queryParam("eventName", eventName)
                .queryParam("eventDate", eventDate.format(
                        DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH))
                )
                .build()
                .toUriString();
        CrawledPrevEvent crawledPrevEvent = handleGetRequest(url, CrawledPrevEvent.class);
        updateCompletedFightEvent(crawledPrevEvent, eventToMarkComplete);
    }


    /**
     * 이미 completed 처리된 이벤트의 FotN/PotN 데이터만 갱신.
     * syncFightEvents()는 completed 이벤트를 건너뛰므로, 보너스 폴링 전용으로 사용.
     *
     * @return 하나라도 FotN/PotN이 존재하면 true
     */
    @Loggable
    @Transactional
    public boolean syncBonusDataForEvent(Long eventId) {
        FightEvent event = fightEventRepository.findByIdWithFfes(eventId)
                .orElseThrow(() -> new CustomException(SERVER_ERROR_500));
        String url = UriComponentsBuilder.fromHttpUrl(flaskURI + "/prev_event")
                .queryParam("eventName", event.getName())
                .queryParam("eventDate", event.getEventDate().format(
                        DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH)))
                .build()
                .toUriString();
        CrawledPrevEvent crawledPrevEvent = handleGetRequest(url, CrawledPrevEvent.class);
        for (CrawledFightCard card : crawledPrevEvent.crawledFightCards()) {
            if (card.isFotN() || card.isPotN()) {
                return true;
            }
        }
        return false;
    }

    private void updateCompletedFightEvent(CrawledPrevEvent crawledPrevEvent, FightEvent eventToMarkComplete) {
        for (CrawledFightCard card : crawledPrevEvent.crawledFightCards()) {
            for (FighterFightEvent match : eventToMarkComplete.getFighterFightEvents()) {
                if (card.winnerName().equals(match.getWinner().getName()) ||
                        card.winnerName().equals(match.getLoser().getName())) {
                    match.updateFromCrawledPrevFight(card.buildFightResult(),
                            card.draw(), card.nc(), card.isFotN(), card.isPotN());
                    if (!card.winnerName().equals(match.getWinner().getName())) {
                        match.swapWinnerAndLoser();
                    }
                }
            }
        }
        if (!crawledPrevEvent.eventName().equals(eventToMarkComplete.getName())) {
            log.warn("EventName different. Update {} to {}", eventToMarkComplete.getName(), crawledPrevEvent.eventName());
            eventToMarkComplete.updateName(crawledPrevEvent.eventName());
        }
        eventToMarkComplete.updateFightEventToCompleted();
    }

    private void saveOrUpdateFighters(List<FighterCrawlerDto> fighterDtos,
                                      Map<String, Fighter> crawledFighterNameMap) {
        for (FighterCrawlerDto dto : fighterDtos) {
            Fighter existing = crawledFighterNameMap.get(dto.name());
            if (existing != null) {
                if (!existing.getFightRecord().equals(toFightRecord(dto.record().split("-")))) {
                    existing.updateFightRecord(dto.record().split("-"));
                }
            } else {
                Fighter savedFighter = fighterRepository.save(dto.toEntity());
                crawledFighterNameMap.put(savedFighter.getName(), savedFighter);
            }
        }
        log.info("=======save fighters completed========");
    }

    private void saveNewUpcomingEvents(List<EventCrawlerDto> crawledUpcomingEvents,
                                       List<FightEvent> existingUpcomingEvents,
                                       Map<String, Fighter> crawledFighterNameMap) {
        for (EventCrawlerDto crawledUpcomingEvent : crawledUpcomingEvents) {
            FightEvent newEvent = crawledUpcomingEvent.toEntityUpcomingEvent();
            if (newEvent.getMainCardDateTimeInfo() == null) {
                adminPushNotificationService.sendNotificationToAdmin(
                        "New Event MainCardDateTimeInfo is null. EventName=" + newEvent.getName());
            }
            // 1. DB의 upcoming event에 crawling으로 불러온 upcoming event가 포함되지 않는 경우 => 이는 새로 생긴 upcoming event
            // 2. event name 이 같더라도, event 내부의 fighter fight event 내용 다를 경우 => 이는 기존 event 내용 변경된 케이스
            FightEvent existingUpcomingEvent = existingUpcomingEvents.stream()
                    .filter((existing) -> existing.getEventDate().equals(newEvent.getEventDate()))
                    .findFirst()
                    .orElse(null);
            if (existingUpcomingEvent == null) {
                saveUpcomingEvent(crawledUpcomingEvent, newEvent, crawledFighterNameMap);
            } else {
                // 기존(DB)의 다가오는 이벤트 정보 - 현재 불러온 해당 다가오는 이벤트 정보를 비교하여 바뀌었으면, 내용물 변경
                boolean isChanged = isEventContentDifferent(
                        crawledUpcomingEvent, existingUpcomingEvent, newEvent, crawledFighterNameMap);
                if (isChanged) {
                    log.info("Weekly event content changed");
                    adminPushNotificationService.sendNotificationToAdmin("Weekly event content changed");
                    updateUpcomingEventsContent(crawledUpcomingEvent, existingUpcomingEvent, crawledFighterNameMap);
                }
                LocalDate newDisplayDate = FightEventUtils.resolveDisplayDate(
                        newEvent.getEarlyCardDateTimeInfo(),
                        newEvent.getPrelimCardDateTimeInfo(),
                        newEvent.getMainCardDateTimeInfo());
                if (newDisplayDate != null)
                    existingUpcomingEvent.updateDisplayDate(newDisplayDate);
                else
                    adminPushNotificationService.sendNotificationToAdmin(crawledUpcomingEvent.eventName() + " display date is null");
            }
        }
    }

    private void updateUpcomingEventsContent(EventCrawlerDto crawledEvent, FightEvent existingUpcomingEvent,
                                             Map<String, Fighter> fighterMap) {
        Map<String, FighterFightEvent> existingMap = new HashMap<>();
        for (FighterFightEvent ffe : existingUpcomingEvent.getFighterFightEvents()) {
            String key = ffe.getWinner().getName() + "_" + ffe.getLoser().getName();
            existingMap.put(key, ffe);
        }
        // this for loop handles card to insert or reactivate
        for (int i = 0; i < crawledEvent.crawledFightCards().size(); i++) {
            CrawledFightCard crawledCard = crawledEvent.crawledFightCards().get(i);
            String key = crawledCard.winnerName() + "_" + crawledCard.loserName();

            FighterFightEvent ffe = existingMap.get(key);
            if (ffe == null) {
                Fighter winner = fighterMap.get(crawledCard.winnerName());
                Fighter loser = fighterMap.get(crawledCard.loserName());
                ffe = FighterFightEvent.builder()
                        .winner(winner)
                        .loser(loser)
                        .fightWeight(FightWeight.fromDisplayName(crawledCard.fightWeight()))
                        .title(crawledCard.title())
                        .build();
                existingUpcomingEvent.addFighterFightEvent(ffe);
            } else if (ffe.isCanceled()) {
                ffe.reactivate(i + 1);
                continue;
            }
            ffe.updateCardOrder(i + 1);
            ffe.updateBonuses(crawledCard.isFotN(), crawledCard.isPotN());
        }

        // this nested for loop handles card to cancel
        // for DB, this does not remove card. just changes state to canceled
        for (FighterFightEvent existingFfe : existingUpcomingEvent.getFighterFightEvents()) {
            if (existingFfe.isCanceled()) continue;
            boolean crawledCardContainsExistingFfe = false;
            for (CrawledFightCard crawledCard : crawledEvent.crawledFightCards()) {
                if (crawledCard.winnerName().equals(existingFfe.getWinner().getName())
                        && crawledCard.loserName().equals(existingFfe.getLoser().getName())) {
                    crawledCardContainsExistingFfe = true;
                    break;
                }
            }
            if (!crawledCardContainsExistingFfe) {
                existingFfe.updateStatusToCanceled();
                log.warn("canceled card={} VS {}", existingFfe.getWinner().getName(), existingFfe.getLoser().getName());
            }
        }
    }

    private boolean isEventContentDifferent(EventCrawlerDto dto, FightEvent existingEvent,
                                            FightEvent newEvent, Map<String, Fighter> crawledFighterNameMap) {
        List<FighterFightEvent> existingCards = existingEvent.getFighterFightEvents()
                .stream().filter(ffe -> !ffe.isCanceled()).toList();
        dto.crawledFightCards().forEach(
                card -> {
                    Fighter winner = crawledFighterNameMap.get(card.winnerName());
                    Fighter loser = crawledFighterNameMap.get(card.loserName());
                    newEvent.addFighterFightEvent(card.toEntity(winner, loser, 1));
                }
        );
        List<FighterFightEvent> crawledEventCards = newEvent.getFighterFightEvents();

        if (existingCards.size() != crawledEventCards.size()) {
            return true;
        }

        for (FighterFightEvent existingFfe : existingCards) {
            String winnerName = existingFfe.getWinner().getName();
            String loserName = existingFfe.getLoser().getName();
            boolean found = crawledEventCards.stream().anyMatch(newFfe ->
                    newFfe.getWinner().getName().equals(winnerName) &&
                            newFfe.getLoser().getName().equals(loserName));
            if (!found)
                return true;
        }
        for (CrawledFightCard crawledCard : dto.crawledFightCards()) {
            for (FighterFightEvent existingFfe : existingCards) {
                if (existingFfe.getWinner().getName().equals(crawledCard.winnerName()) &&
                        existingFfe.getLoser().getName().equals(crawledCard.loserName())) {
                    if (existingFfe.isFotN() != crawledCard.isFotN() || existingFfe.isPotN() != crawledCard.isPotN())
                        return true;
                    break;
                }
            }
        }
        return false;
    }

    private void saveUpcomingEvent(EventCrawlerDto dto, FightEvent event, Map<String, Fighter> crawledFighterNameMap) {
        log.info("==save upcoming event== eventName={}", event.getName());
        int cardOrder = 1;
        for (CrawledFightCard card : dto.crawledFightCards()) {
            Fighter winner = crawledFighterNameMap.get(card.winnerName());
            Fighter loser = crawledFighterNameMap.get(card.loserName());
            FighterFightEvent fight = card.toEntity(winner, loser, cardOrder++);
            event.addFighterFightEvent(fight);
        }
        fightEventRepository.save(event);
    }

    private <T> T handleGetRequest(String path, Class<T> clazz) {
        try {
            T result = restTemplate.getForObject(path, clazz);
            if (result == null) throw new CustomException(SERVER_ERROR_500);
            return result;
        } catch (ResourceAccessException e) {
            log.error("Flask 서버 연결 실패: {}", e.getMessage());
            adminPushNotificationService.sendNotificationToAdmin("Flask server connection failed");
            throw new CustomException(SERVER_ERROR_500);
        } catch (HttpStatusCodeException e) {
            log.error("Flask 서버 오류 응답: status={}", e.getStatusCode());
            adminPushNotificationService.sendNotificationToAdmin("Error while requesting fightEvent to flask");
            throw new CustomException(SERVER_ERROR_500);
        }
    }
}
