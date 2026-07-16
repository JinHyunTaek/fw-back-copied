package my.mma.api.event.common.dto;

import lombok.Builder;
import my.mma.api.event.common.dto.enumtype.EventType;
import my.mma.api.event.promotion.entity.Promotion;

import java.time.LocalDate;
import java.util.List;

public record EventCardsDto(
        List<EventCardDto> eventCards
) {
    @Builder
    public record EventCardDto(
            String type,
            Long refId,
            String title,
            String benefit,
            LocalDate startDate,
            LocalDate endDate
    ) {
        public static EventCardDto fromPromotion(Promotion promotion){
            return EventCardDto.builder()
                    .type(EventType.PROMOTION.getDescription())
                    .refId(promotion.getId())
                    .title(promotion.getTitle())
                    .benefit(promotion.getBenefit())
                    .startDate(promotion.getStartDate())
                    .endDate(promotion.getEndDate())
                    .build();
        }
    }
}
