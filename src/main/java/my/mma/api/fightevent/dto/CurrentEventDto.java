package my.mma.api.fightevent.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fightevent.dto.abs.IFighterFightEvent;
import my.mma.api.fightevent.dto.abs.IFightEventDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fighter.entity.Fighter;

import java.time.LocalDate;
import java.util.Comparator;

import static my.mma.api.fightevent.dto.CurrentEventDto.*;
import static my.mma.api.fightevent.dto.StreamFighterFightEventStatus.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CurrentEventDto extends IFightEventDto<CurrentFighterFightEventDto> {

    public static CurrentEventDto toDto(FightEvent fightEvent) {
        return CurrentEventDto.builder()
                .id(fightEvent.getId())
                .displayDate(fightEvent.getDisplayDate())
                .name(fightEvent.getName())
                .mainCardDateTimeInfo(fightEvent.getMainCardDateTimeInfo() != null ?
                        CardStartDateTimeInfoDto.toDto(fightEvent.getMainCardDateTimeInfo()) : null)
                .prelimCardDateTimeInfo(fightEvent.getPrelimCardDateTimeInfo() != null ?
                        CardStartDateTimeInfoDto.toDto(fightEvent.getPrelimCardDateTimeInfo()) : null)
                .earlyCardDateTimeInfo(fightEvent.getEarlyCardDateTimeInfo() != null ?
                        CardStartDateTimeInfoDto.toDto(fightEvent.getEarlyCardDateTimeInfo()) : null)
                .mainCardCnt(fightEvent.getMainCardCnt() != null ? fightEvent.getMainCardCnt() : null)
                .prelimCardCnt(fightEvent.getPrelimCardCnt() != null ? fightEvent.getPrelimCardCnt() : null)
                .earlyCardCnt(fightEvent.getEarlyCardCnt() != null ? fightEvent.getEarlyCardCnt() : null)
                .location(fightEvent.getLocation())
                .fighterFightEvents(fightEvent.getFighterFightEvents().stream()
                        .filter(ffe -> !ffe.isCanceled())
                        .sorted(Comparator.comparingInt(FighterFightEvent::getCardOrder))
                        .map(ffe -> CurrentFighterFightEventDto.toDto(ffe, UPCOMING))
                        .toList())
                .build();
    }

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CurrentFighterFightEventDto extends IFighterFightEvent<FighterFightEventCardFighterDto> {

        private StreamFighterFightEventStatus status;

        private int firstFighterBetCount;

        private int lastFighterBetCount;

        // for streaming (upcoming) event -> result is null
        public static CurrentFighterFightEventDto toDto(FighterFightEvent ffe, StreamFighterFightEventStatus status) {
            return CurrentFighterFightEventDto.builder()
                    .id(ffe.getId())
                    .status(status)
                    .eventName(ffe.getFightEvent().getName())
                    .fightWeight(ffe.getFightWeight() != null ? ffe.getFightWeight().getDisplayName() : null)
                    .winner(FighterFightEventCardFighterDto.of(ffe.getWinner()))
                    .loser(FighterFightEventCardFighterDto.of(ffe.getLoser()))
                    .result(null)
                    .title(ffe.isTitle())
                    .build();
        }

        public void updateFighterInfo(Fighter fighter, boolean isWinner) {
            if(isWinner){
                this.winner = FighterFightEventCardFighterDto.of(fighter);
            }else{
                this.loser = FighterFightEventCardFighterDto.of(fighter);
            }
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @SuperBuilder
    public static class FighterFightEventCardFighterDto extends FighterDto {

        private LocalDate birthday;

        private int reach;

        private int height;

//        @Setter
//        private String bodyUrl;

        private Double weight;

        public static FighterFightEventCardFighterDto of(Fighter fighter) {
            return toDto(fighter,
                    builder()
                            .reach(fighter.getReach())
                            .height(fighter.getHeight())
                            .weight(fighter.getWeight())
                            .birthday(fighter.getBirthday())
            );
        }
    }

}
