package my.mma.api.fightevent.dto.abs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;

import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = true)
public abstract class IFightEventDto<T extends IFighterFightEvent<? extends FighterDto>> {

    protected long id;

    protected LocalDate displayDate;

    protected CardStartDateTimeInfoDto mainCardDateTimeInfo;

    protected CardStartDateTimeInfoDto prelimCardDateTimeInfo;

    protected CardStartDateTimeInfoDto earlyCardDateTimeInfo;

    protected Integer mainCardCnt;

    protected Integer prelimCardCnt;

    protected Integer earlyCardCnt;

    protected String location;

    protected String name;

    protected List<T> fighterFightEvents;

}
