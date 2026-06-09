package my.mma.fixture.entity.fightevent;

import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.property.CardStartDateTimeInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static my.mma.fixture.entity.fighterfightevent.FighterFightEventFixture.upcomingWithId;

public class FightEventFixture {

    private static final int TOTAL_CARD_COUNT = 12;

    public static FightEventBuilder builder(long id) {
        return new FightEventBuilder(id);
    }

    public static FightEvent createUpcomingFightEventWithId(long id) {
        return builder(id).build();
    }

    public static FightEvent createFightEventForPersist(int year) {
        LocalDate date = LocalDate.of(year, 10, 10);
        CardStartDateTimeInfo dtInfo = CardStartDateTimeInfo.builder()
                .date(date)
                .time(LocalTime.of(3, 0, 0))
                .build();
        return FightEvent.builder()
                .name(UUID.randomUUID().toString())
                .eventDate(date)
                .displayDate(date)
                .location("location-test")
                .completed(false)
                .mainCardCnt(1).mainCardDateTimeInfo(dtInfo)
                .prelimCardCnt(1).prelimCardDateTimeInfo(dtInfo)
                .earlyCardCnt(1).earlyCardDateTimeInfo(dtInfo)
                .build();
    }

    public static class FightEventBuilder {
        private final long id;
        private boolean completed = false;
        private boolean withFFEs = true;
        private LocalDateTime main = LocalDateTime.now().plusDays(3);
        private LocalDateTime prelim = main.minusHours(3);
        private LocalDateTime early = prelim.minusHours(3);
        private LocalDate displayDate = early.toLocalDate();

        private FightEventBuilder(long id) {
            this.id = id;
        }

        public FightEventBuilder displayDate(LocalDate displayDate){
            this.displayDate = displayDate;
            return this;
        }

        public FightEventBuilder mainDT(LocalDateTime main) {
            this.main = main;
            return this;
        }

        public FightEventBuilder prelimDT(LocalDateTime prelim) {
            this.prelim = prelim;
            return this;
        }

        public FightEventBuilder earlyDT(LocalDateTime early) {
            this.early = early;
            return this;
        }

        public FightEventBuilder completed() {
            this.completed = true;
            return this;
        }

        public FightEventBuilder withoutFFEs() {
            this.withFFEs = false;
            return this;
        }

        public FightEvent build() {
            LocalDateTime baseDt = early != null ? early : (prelim != null ? prelim : main);

            FightEvent fightEvent = FightEvent.builder()
                    .id(id)
                    .name("UFC-" + id)
                    .eventDate(baseDt.toLocalDate())
                    .displayDate(displayDate)
                    .completed(completed)
                    .location("location-" + id)
                    .mainCardCnt(main != null ? TOTAL_CARD_COUNT / 2 : null)
                    .mainCardDateTimeInfo(main != null ? toCardStartDateTimeInfo(main) : null)
                    .prelimCardCnt(prelim != null ? TOTAL_CARD_COUNT / 3 : null)
                    .prelimCardDateTimeInfo(prelim != null ? toCardStartDateTimeInfo(prelim) : null)
                    .earlyCardCnt(early != null ? TOTAL_CARD_COUNT / 6 : null)
                    .earlyCardDateTimeInfo(early != null ? toCardStartDateTimeInfo(early) : null)
                    .build();

            if (withFFEs) {
                for (int i = 0; i < TOTAL_CARD_COUNT; i++) {
                    fightEvent.getFighterFightEvents().add(upcomingWithId(fightEvent, i));
                }
            }

            return fightEvent;
        }

        private CardStartDateTimeInfo toCardStartDateTimeInfo(LocalDateTime localDateTime) {
            return CardStartDateTimeInfo.builder()
                    .date(localDateTime.toLocalDate())
                    .time(localDateTime.toLocalTime())
                    .build();
        }
    }
}
