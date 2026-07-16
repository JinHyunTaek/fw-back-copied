package my.mma.api.event.promotion.dto;

public record PromotionDetailResponse(
        PromotionDetailDto promotion,
        int myEntryCount,
        int entryCap
) {}
