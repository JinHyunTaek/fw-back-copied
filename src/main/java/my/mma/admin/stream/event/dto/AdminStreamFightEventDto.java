package my.mma.admin.stream.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import my.mma.api.fightevent.dto.FightResultDto;

import java.util.List;

/**
 * 경기 진행 시 실시간으로 Flask Server가 응답하는 FightEvent 데이터 포멧
 */
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminStreamFightEventDto {

    private String name;

    private List<AdminStreamFighterFightEventDto> streamFighterFightEvents;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AdminStreamFighterFightEventDto{

        @JsonProperty("fight_weight")
        private String fightWeight;

        @JsonProperty("winner_name")
        private String winnerName;

        @JsonProperty("loser_name")
        private String loserName;

        @JsonProperty("result")
        private FightResultDto result;

        private boolean nc;

        private boolean draw;

    }

}
