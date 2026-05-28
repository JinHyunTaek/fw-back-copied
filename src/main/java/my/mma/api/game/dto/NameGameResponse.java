package my.mma.api.game.dto;

import lombok.*;
import my.mma.api.fighter.dto.RankersPerCategory.RankingCategory;
import my.mma.api.fighter.entity.Country;
import my.mma.api.fighter.entity.FightRecord;

import java.util.List;

@Builder
public record NameGameResponse(
        NameGameCategory nameGameCategory,
        // name은 어떤 category 든지 무조간 들어가야 됨
        String answer,
        String nickname,
        Integer ranking,
        RankingCategory rankingCategory, // pfp / 체급
        Country nationality,
        List<String> opponents,
        FightRecord fightRecord,
        List<String> wrongSelections
) {

}
