package my.mma.admin.web.promotion.dto;

import java.time.LocalDate;

public record AdminPromotionResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate announceDate,
        LocalDate createdDate
) {
}
