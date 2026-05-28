package my.mma.api.game.dto;

import lombok.*;

@Builder
public record GameAttemptResponse(int count, int adCount) {}
