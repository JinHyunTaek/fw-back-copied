package my.mma.admin.event.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;
import my.mma.api.fightevent.entity.FightEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static my.mma.api.fightevent.utils.FightEventUtils.*;
import static my.mma.api.global.utils.CustomUnitUtils.toCentimeter;
import static my.mma.api.global.utils.ModifyUtils.toKg;

/**
 * basically getting upcoming event & prev event for updating previous event
 */
public record CrawledUpcomingEvent(List<FighterCrawlerDto> fighters, List<EventCrawlerDto> events) {

    public record FighterCrawlerDto(
            String name,
            String record,
            String weight,
            String height,
            String nickname,
            String reach,
            String birthday
    ) {
        public Fighter toEntity() {
            String[] split_record = record.split("-");
            return Fighter.builder()
                    .weight(weight != null ? toKg(this.weight) : null)
                    .height(toCentimeter(this.height))
                    .name(this.name())
                    .fightRecord(
                            FightRecord.builder()
                                    .win(Integer.parseInt(split_record[0]))
                                    .loss(Integer.parseInt(split_record[1]))
                                    .draw(((int) split_record[2].charAt(0)) - 48)
                                    .build()
                    )
                    .nickname(this.nickname())
                    .reach(this.reach.contains("-") ? 0 : Integer.parseInt(this.reach))
                    .birthday(LocalDate.parse(this.birthday, DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)))
                    .build();
        }

    }

    public record EventCrawlerDto(
            @JsonProperty("event_name")
            String eventName,

            @JsonProperty("event_date")
            String eventDate,

            @JsonProperty("main_card_info")
            CardStartDateTimeInfoDto mainCardDateTimeInfo,

            @JsonProperty("prelim_card_info")
            CardStartDateTimeInfoDto prelimCardDateTimeInfo,

            @JsonProperty("early_card_info")
            CardStartDateTimeInfoDto earlyCardDateTimeInfo,

            @JsonProperty("main_card_cnt")
            Integer mainCardCnt,

            @JsonProperty("prelim_card_cnt")
            Integer prelimCardCnt,

            @JsonProperty("early_card_cnt")
            Integer earlyCardCnt,

            String location,

            @JsonProperty("cards")
            List<CrawledFightCard> crawledFightCards
    ) {

        // 단순 이벤트 날짜만 비교하는 용도로 사용되는 빌더
        public FightEvent toEntityForEventName() {
            return FightEvent.builder()
                    .location(location)
                    .eventDate(LocalDate.parse(this.eventDate, DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH)))
                    .name(this.eventName)
                    .build();
        }

        public FightEvent toEntityUpcomingEvent() {
            LocalDate localEventDate = LocalDate.parse(this.eventDate, DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH));
            LocalDate displayDate = resolveDisplayDateFromDtos(earlyCardDateTimeInfo, prelimCardDateTimeInfo, mainCardDateTimeInfo);

            return FightEvent.builder()
                    .mainCardDateTimeInfo(this.mainCardDateTimeInfo != null ? this.mainCardDateTimeInfo.toDto() : null)
                    .prelimCardDateTimeInfo(this.prelimCardDateTimeInfo != null ? this.prelimCardDateTimeInfo.toDto() : null)
                    .earlyCardDateTimeInfo(this.earlyCardDateTimeInfo != null ? this.earlyCardDateTimeInfo.toDto() : null)
                    .mainCardCnt(this.mainCardCnt)
                    .prelimCardCnt(this.prelimCardCnt)
                    .earlyCardCnt(this.earlyCardCnt)
                    .completed(false)
                    .location(location)
                    .eventDate(localEventDate)
                    .displayDate(displayDate != null ? displayDate : localEventDate)
                    .name(eventName)
                    .build();
        }
    }
}
