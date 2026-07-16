package my.mma.api.event.promotion.dto;

import lombok.Builder;
import my.mma.api.event.promotion.entity.Promotion;

import java.time.LocalDate;
import java.util.List;

public record HomePromotionsDto(List<HomePromotionDto> homePromotions){

    @Builder
    public record HomePromotionDto(
            Long id,
            String title,
            String benefit,
            LocalDate startDate,
            LocalDate endDate,
            int maxWinnerCount
    ) {
        public static HomePromotionDto toDto(Promotion promotion){
            return HomePromotionDto.builder()
                    .id(promotion.getId())
                    .title(promotion.getTitle())
                    .benefit(promotion.getBenefit())
                    .startDate(promotion.getStartDate())
                    .endDate(promotion.getEndDate())
                    .maxWinnerCount(promotion.getMaxWinnerCount())
                    .build();
        }
    }

}
