package my.mma.api.game.dto;

import lombok.*;

import java.util.List;

@Builder
public record ImageGameResponse(
        String name,
        String answer,
        List<String> wrongSelections
) {}
