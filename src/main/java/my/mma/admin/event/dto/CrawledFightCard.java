package my.mma.admin.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.FightResult;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fightevent.entity.property.WinMethod;
import my.mma.api.global.utils.CustomDateUtils;

public record CrawledFightCard(
        @JsonProperty("winner")
        String winnerName,

        @JsonProperty("loser")
        String loserName,

        WinMethod method,

        String description,

        @JsonProperty("fight_weight")
        String fightWeight,

        boolean title,

        @JsonProperty("is_fotn")
        boolean isFotN,

        @JsonProperty("is_potn")
        boolean isPotN,

        String round,

        @JsonProperty("fight_time")
        String fightTime,

        boolean draw,

        boolean nc
) {
    // for upcoming fight event ( & comparing existing-crawled upcoming event)
    public FighterFightEvent toEntity(Fighter winner, Fighter loser, int cardOrder) {
        return FighterFightEvent.builder()
                .cardOrder(cardOrder)
                .title(title)
                .winner(winner)
                .loser(loser)
                .fightWeight(FightWeight.fromDisplayName(fightWeight))
                .fightResult(null)
                .build();
    }

    public FightResult buildFightResult() {
        String[] timeParts = fightTime.split(":");
        int round = Integer.parseInt(this.round);
        return FightResult.builder()
                .winMethod(method)
                .winDescription(description)
                .fightDuration(CustomDateUtils.getFightDuration(round, timeParts))
                .round(round)
                .build();
    }
}
