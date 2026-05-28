package my.mma.fixture.dto.fightevent;

import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;
import my.mma.api.fightevent.dto.FightEventDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static my.mma.fixture.dto.fightevent.FighterFightEventDtoFixture.createUpcomingFfeDtoWithId;

public class FightEventDtoFixture {

    private static final int TOTAL_CARD_COUNT = 12;

    public static FightEventDto createUpcomingFightEventDtoWithIdAndDisplayDate(int id, String headshotUrl, LocalDate displayDate) {
        FightEventDto fightEventDto = getFightEventDtoWithNumAndDisplayDateWithoutFfeAndDisplayDate(id, displayDate);
        for (int i = 0; i < TOTAL_CARD_COUNT; i++) {
            fightEventDto.getFighterFightEvents().add(createUpcomingFfeDtoWithId(fightEventDto, i, headshotUrl));
        }
        return fightEventDto;
    }

    private static FightEventDto getFightEventDtoWithNumAndDisplayDateWithoutFfeAndDisplayDate(int id, LocalDate displayDate) {
        LocalDateTime main = LocalDateTime.of(2000,1,1,10,0,0);
        LocalDateTime prelim = main.plusHours(4);
        LocalDateTime early = prelim.plusHours(4);

        return FightEventDto.builder()
                .id(id)
                .name("UFC-" + id)
                .displayDate(displayDate)
                .upcoming(true)
                .location("location-" + id)
                .mainCardCnt(TOTAL_CARD_COUNT / 2)
                .mainCardDateTimeInfo(createCardStartDateTimeInfoDtoWithLocalDateTime(main))
                .prelimCardCnt(TOTAL_CARD_COUNT / 3)
                .prelimCardDateTimeInfo(createCardStartDateTimeInfoDtoWithLocalDateTime(prelim))
                .earlyCardCnt(TOTAL_CARD_COUNT / 6)
                .earlyCardDateTimeInfo(createCardStartDateTimeInfoDtoWithLocalDateTime(early))
                .fighterFightEvents(new ArrayList<>())
                .build();
    }

    private static CardStartDateTimeInfoDto createCardStartDateTimeInfoDtoWithLocalDateTime(LocalDateTime localDateTime) {
        return CardStartDateTimeInfoDto.builder()
                .date(localDateTime.toLocalDate())
                .time(localDateTime.toLocalTime())
                .build();
    }

}
