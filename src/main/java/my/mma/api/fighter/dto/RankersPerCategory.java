package my.mma.api.fighter.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;

import java.util.List;

public record RankersPerCategory(@JsonProperty("rankers")
                         List<RankerDto> rankers) {

    @Setter
    @Getter
    public static class RankerDto {

        private Long id;

        private String name;

        private String koreanName;

        private RankingCategory category;

        private String headshotUrl;

        private int ranking;

        public void updateFromFighter(Fighter fighter, String headshotUrl){
            this.id = fighter.getId();
            this.koreanName = fighter.getKoreanName();
            this.headshotUrl = headshotUrl;
        }

    }

    @Getter
    @RequiredArgsConstructor
    public enum RankingCategory{

        @JsonProperty("MENS_POUND_FOR_POUND_TOP_RANK")
        MENS_POUND_FOR_POUND_TOP_RANK,
        @JsonProperty("플라이급")
        FLYWEIGHT,
        @JsonProperty("밴텀급")
        BANTAMWEIGHT,
        @JsonProperty("페더급")
        FEATHERWEIGHT,
        @JsonProperty("라이트급")
        LIGHTWEIGHT,
        @JsonProperty("웰터급")
        WELTERWEIGHT,
        @JsonProperty("미들급")
        MIDDLEWEIGHT,
        @JsonProperty("라이트_헤비급")
        LIGHT_HEAVYWEIGHT,
        @JsonProperty("헤비급")
        HEAVYWEIGHT,
        @JsonProperty("WOMENS_POUND_FOR_POUND_TOP_RANK")
        WOMENS_POUND_FOR_POUND_TOP_RANK,
        @JsonProperty("여성_스트로급")
        WOMENS_STRAWWEIGHT,
        @JsonProperty("여성_플라이급")
        WOMENS_FLYWEIGHT,
        @JsonProperty("여성_밴텀급")
        WOMENS_BANTAMWEIGHT;

    }

}
