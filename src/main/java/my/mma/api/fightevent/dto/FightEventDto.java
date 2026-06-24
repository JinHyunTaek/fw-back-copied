package my.mma.api.fightevent.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fightevent.dto.abs.IFighterFightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.dto.abs.IFightEventDto;
import my.mma.api.fightevent.entity.FightEvent;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 크롤링 시 사용되지 않음 (admin의 실시간 이벤트 및 주기적인 upcoming, previous event update용 CrawlerDto와 무관)
 * 오직 클라이언트와 이벤트 정보 송수신 시 사용되는 dto
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class FightEventDto extends IFightEventDto<FightEventDto.FighterFightEventDto> {

    private boolean upcoming;

    @Setter
    private boolean alert;

    public static FightEventDto toDto(FightEvent fightEvent) {
        return FightEventDto.builder()
                .id(fightEvent.getId())
                .alert(false)
                .displayDate(fightEvent.getDisplayDate())
                .mainCardDateTimeInfo(fightEvent.getMainCardDateTimeInfo() != null ?
                        CardStartDateTimeInfoDto.toDto(fightEvent.getMainCardDateTimeInfo()) : null)
                .prelimCardDateTimeInfo(fightEvent.getPrelimCardDateTimeInfo() != null ?
                        CardStartDateTimeInfoDto.toDto(fightEvent.getPrelimCardDateTimeInfo()) : null)
                .earlyCardDateTimeInfo(fightEvent.getEarlyCardDateTimeInfo() != null ?
                        CardStartDateTimeInfoDto.toDto(fightEvent.getEarlyCardDateTimeInfo()) : null)
                .mainCardCnt(fightEvent.getMainCardCnt())
                .prelimCardCnt(fightEvent.getPrelimCardCnt())
                .earlyCardCnt(fightEvent.getEarlyCardCnt())
                .location(fightEvent.getLocation())
                .name(fightEvent.getName())
                .upcoming(!fightEvent.isCompleted())
                .fighterFightEvents(fightEvent.getFighterFightEvents().stream()
                        .sorted(Comparator.comparingInt(FighterFightEvent::getCardOrder))
                        .map(
                                FightEventDto.FighterFightEventDto::toDto
                        ).collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @SuperBuilder
    public static class FighterFightEventDto extends IFighterFightEvent<FighterDto> {

        private Long eventId;
        private LocalDate displayDate;
        private boolean isFotN;
        private boolean isPotN;

        public static FighterFightEventDto toDto(FighterFightEvent fighterFightEvent) {
            return FightEventDto.FighterFightEventDto.builder()
                    .id(fighterFightEvent.getId())
                    .eventId(fighterFightEvent.getFightEvent().getId())
                    .displayDate(fighterFightEvent.getFightEvent().getDisplayDate())
                    .eventName(fighterFightEvent.getFightEvent().getName())
                    .fightWeight(fighterFightEvent.getFightWeight() != null ? fighterFightEvent.getFightWeight().getDisplayName() : null)
                    .result(fighterFightEvent.getFightResult() != null ? FightResultDto.toDto(fighterFightEvent.getFightResult()) : null)
                    .winner(FighterDto.toDto(fighterFightEvent.getWinner()))
                    .loser(FighterDto.toDto(fighterFightEvent.getLoser()))
                    .title(fighterFightEvent.isTitle())
                    .isFotN(fighterFightEvent.isFotN())
                    .isPotN(fighterFightEvent.isPotN())
                    .build();
        }

    }

}
