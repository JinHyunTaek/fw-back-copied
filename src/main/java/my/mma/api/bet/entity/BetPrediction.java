package my.mma.api.bet.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import lombok.*;
import my.mma.api.fightevent.entity.property.WinMethod;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Embeddable
@Builder
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class BetPrediction {

    // 실제 승자 x, 내가 선택한 승자
    private Long myWinnerId;

    // 실제 패자 x, 내가 선택한 패자
    private Long myLoserId;

    // if draw) winnerId, loserId, draw, winRound would be replaced to null
    private boolean draw;

    private Integer finishRound;

    @Enumerated(STRING)
    private WinMethod winMethod;

    @JsonProperty("isFotN")
    private boolean isFotN;

    @JsonProperty("isPotN")
    private boolean isPotN;

    public void updateFromDrawRequest(){
        this.myWinnerId = null;
        this.myLoserId = null;
        this.winMethod = null;
        this.finishRound = null;
    }

    public void updateFromDecRequest() {
        this.finishRound = null;
    }
}
