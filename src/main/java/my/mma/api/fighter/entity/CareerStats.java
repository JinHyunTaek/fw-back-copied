package my.mma.api.fighter.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import my.mma.api.fighter.dto.CareerStatsDto;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareerStats {

    /**
     * Career statistics:
     * SLpM: 4.43
     * Str. Acc.: 43%
     * SApM: 3.82
     * Str. Def: 55%
     * TD Avg.: 2.14
     * TD Acc.: 36%
     * TD Def.: 90%
     * Sub. Avg.: 0.1
     */

    private Double significantStrikesLandedPerMin;

    private Double significantStrikesAbsorbedPerMin;

    private Double takedownAvgPer15Min;

    private Double submissionAvgPer15Min;

    private Integer strikingAccuracyPct;

    private Integer strikingDefencePct;

    private Integer takedownAccuracyPct;

    private Integer takedownDefencePct;

    public static CareerStats of(CareerStatsDto dto){
        return CareerStats.builder()
                .significantStrikesLandedPerMin(dto.significantStrikesLandedPerMin())
                .significantStrikesAbsorbedPerMin(dto.significantStrikesAbsorbedPerMin())
                .takedownAvgPer15Min(dto.takedownAvgPer15Min())
                .submissionAvgPer15Min(dto.submissionAvgPer15Min())
                .strikingAccuracyPct(dto.strikingAccuracyPct())
                .strikingDefencePct(dto.strikingDefencePct())
                .takedownAccuracyPct(dto.takedownAccuracyPct())
                .takedownDefencePct(dto.takedownDefencePct())
                .build();
    }

    // AI 선수 상세 툴 직렬화용 (entity -> DTO)
    public CareerStatsDto toDto(){
        return new CareerStatsDto(
                significantStrikesLandedPerMin,
                significantStrikesAbsorbedPerMin,
                takedownAvgPer15Min,
                submissionAvgPer15Min,
                strikingAccuracyPct,
                strikingDefencePct,
                takedownAccuracyPct,
                takedownDefencePct
        );
    }

}
