package my.mma.api.fightevent.dto.abs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fightevent.dto.FightResultDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class IFighterFightEvent<T extends FighterDto> {

    private String eventName;

    protected Long id;

    protected String fightWeight;

    protected T winner;

    protected T loser;

    protected FightResultDto result;

    protected boolean title;

}
