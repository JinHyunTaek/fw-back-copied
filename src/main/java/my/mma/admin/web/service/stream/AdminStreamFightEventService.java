package my.mma.admin.web.service.stream;

import lombok.RequiredArgsConstructor;
import my.mma.admin.event.service.AdminSaveCurrentEventService;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.CardStartDateTimeInfo;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStreamFightEventService {

    private final FightEventRepository fightEventRepository;
    private final FighterFightEventRepository ffeRepository;
    private final FighterRepository fighterRepository;
    private final AdminSaveCurrentEventService saveStreamFightEventService;

    public List<FightEvent> getUpcomingEvents() {
        return fightEventRepository.findByCompletedIsFalse();
    }

    public FightEvent getEventWithFfes(Long eventId) {
        return fightEventRepository.findByIdWithFfes(eventId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_EVENT_FOUND_400));
    }

    @Transactional
    public void updateEventInfo(Long eventId,
                                String name,
                                LocalDate displayDate,
                                LocalDate earlyDate, LocalTime earlyTime,
                                LocalDate prelimDate, LocalTime prelimTime,
                                LocalDate mainDate, LocalTime mainTime) {
        FightEvent event = fightEventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_EVENT_FOUND_400));
        event.updateName(name);
        event.updateDisplayDate(displayDate);
        event.updateCardDateTimeInfos(
                toInfo(earlyDate, earlyTime),
                toInfo(prelimDate, prelimTime),
                toInfo(mainDate, mainTime)
        );
        if (!event.isCompleted()) {
            saveStreamFightEventService.syncStreamFightEvent();
        }
    }

    @Transactional
    public void addFfe(Long eventId, Long winnerId, Long loserId, String fightWeightStr, boolean title, int cardOrder) {
        FightWeight fightWeight = FightWeight.fromDisplayName(fightWeightStr);
        FightEvent event = fightEventRepository.findByIdWithFfes(eventId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_EVENT_FOUND_400));
        Fighter winner = fighterRepository.findById(winnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_FIGHTER_CONFIGURED_400));
        Fighter loser = fighterRepository.findById(loserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_FIGHTER_CONFIGURED_400));

        FighterFightEvent newFfe = FighterFightEvent.builder()
                .winner(winner)
                .loser(loser)
                .fightWeight(fightWeight)
                .title(title)
                .cardOrder(cardOrder)
                .build();
        event.addFighterFightEvent(newFfe);

        if (!event.isCompleted()) {
            saveStreamFightEventService.syncStreamFightEvent();
        }
    }

    @Transactional
    public void cancelFfe(Long ffeId) {
        FighterFightEvent ffe = ffeRepository.findById(ffeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_FIGHT_FOUND_400));
        ffe.updateStatusToCanceled();
        if (!ffe.getFightEvent().isCompleted()) {
            saveStreamFightEventService.syncStreamFightEvent();
        }
    }

    @Transactional
    public void reactivateFfe(Long ffeId) {
        FighterFightEvent ffe = ffeRepository.findById(ffeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_FIGHT_FOUND_400));
        FightEvent event = ffe.getFightEvent();
        int nextOrder = event.getFighterFightEvents().stream()
                .filter(f -> !f.isCanceled())
                .mapToInt(FighterFightEvent::getCardOrder)
                .max().orElse(0) + 1;
        ffe.reactivate(nextOrder);
        if (!event.isCompleted()) {
            saveStreamFightEventService.syncStreamFightEvent();
        }
    }

    @Transactional
    public void updateFfe(Long ffeId, String fightWeightStr, boolean title, int cardOrder) {
        FighterFightEvent ffe = ffeRepository.findById(ffeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_FIGHT_FOUND_400));
        ffe.updateDetails(FightWeight.fromDisplayName(fightWeightStr), title);
        ffe.updateCardOrder(cardOrder);
        if (!ffe.getFightEvent().isCompleted()) {
            saveStreamFightEventService.syncStreamFightEvent();
        }
    }

    private CardStartDateTimeInfo toInfo(LocalDate date, LocalTime time) {
        if (date == null || time == null) return null;
        return CardStartDateTimeInfo.builder().date(date).time(time).build();
    }
}