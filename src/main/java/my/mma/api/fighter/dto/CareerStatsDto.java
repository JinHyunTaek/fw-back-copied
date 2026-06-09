package my.mma.api.fighter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CareerStatsDto(
        @JsonProperty("SLpM")
        Double significantStrikesLandedPerMin,

        @JsonProperty("SApM")
        Double significantStrikesAbsorbedPerMin,

        @JsonProperty("tdAvg")
        Double takedownAvgPer15Min,

        @JsonProperty("subAvg")
        Double submissionAvgPer15Min,

        @JsonProperty("strAcc")
        Integer strikingAccuracyPct,

        @JsonProperty("strDef")
        Integer strikingDefencePct,

        @JsonProperty("tDAcc")
        Integer takedownAccuracyPct,

        @JsonProperty("tdDef")
        Integer takedownDefencePct
) {
}
