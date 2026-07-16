package my.mma.api.event.promotion.dto;

import lombok.Builder;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.GifticonCategory;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.entity.PromotionWinner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PromotionDetailDto(
        String title,
        String benefit,
        LocalDate endDate,
        LocalDate startDate,
        LocalDate announceDate,
        int maxWinnerCount,
        String notice,
        LocalDateTime drawnAt, // nullable
        List<PromotionWinnerGifticonDto> winnerGifticons, // nullable
        List<GifticonDto> gifticons
) {

    public static PromotionDetailDto toDto(
            Promotion promotion,
            List<PromotionWinnerGifticonDto> winnerGifticons,
            List<Gifticon> gifticons){
        return PromotionDetailDto.builder()
                .title(promotion.getTitle())
                .benefit(promotion.getBenefit())
                .endDate(promotion.getEndDate())
                .startDate(promotion.getStartDate())
                .announceDate(promotion.getAnnounceDate())
                .maxWinnerCount(promotion.getMaxWinnerCount())
                .notice(promotion.getNotice())
                .drawnAt(promotion.getDrawnAt())
                .winnerGifticons(winnerGifticons)
                .gifticons(gifticons.stream().map(GifticonDto::toDto).toList())
                .build();
    }

    @Builder
    public record PromotionWinnerGifticonDto(
            String winnerNickname,
            String gifticonName,
            GifticonCategory category
    ){
        public static PromotionWinnerGifticonDto of(PromotionWinner promotionWinner){
            return new PromotionWinnerGifticonDto(
                    promotionWinner.getWinner().getNickname(),
                    promotionWinner.getGifticon().getName(),
                    promotionWinner.getGifticon().getCategory());
        }
    }

    @Builder
    public record GifticonDto(
            String name,
            GifticonCategory category,
            int priority
    ){
        public static GifticonDto toDto(Gifticon gifticon){
            return GifticonDto.builder()
                    .name(gifticon.getName())
                    .category(gifticon.getCategory())
                    .priority(gifticon.getDisplayOrder())
                    .build();
        }
    }
}
