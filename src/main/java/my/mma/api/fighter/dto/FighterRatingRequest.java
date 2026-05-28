package my.mma.api.fighter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FighterRatingRequest(Long fighterId, @Min(1) @Max(5) int rating) {
}
