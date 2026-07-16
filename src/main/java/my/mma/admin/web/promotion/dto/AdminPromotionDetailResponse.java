package my.mma.admin.web.promotion.dto;

import lombok.Builder;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.GifticonCategory;
import my.mma.api.event.promotion.entity.Promotion;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

@Builder
public record AdminPromotionDetailResponse(
        Long id,
        String title,
        String benefit,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate announceDate,
        LocalDate createdDate,
        int maxWinnerCount,
        String notice,
        boolean drawn,       // 추첨 완료 여부 (drawnAt != null) — 당첨자 확인 링크 노출 기준
        List<AdminGifticonResponse> gifticons
) {

    public static AdminPromotionDetailResponse of(Promotion promotion, List<Gifticon> gifticons,
                                                  Function<String, String> imageUrlResolver){
        return AdminPromotionDetailResponse.builder()
                .id(promotion.getId())
                .title(promotion.getTitle())
                .benefit(promotion.getBenefit())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .announceDate(promotion.getAnnounceDate())
                .createdDate(promotion.getCreatedDateTime().toLocalDate())
                .maxWinnerCount(promotion.getMaxWinnerCount())
                .notice(promotion.getNotice())
                .drawn(promotion.isDrawn())
                .gifticons(gifticons.stream()
                        .map(g -> AdminGifticonResponse.of(g, imageUrlResolver.apply(g.getImageKey())))
                        .toList()
                ).build();
    }

    @Builder
    public record AdminGifticonResponse(
            Long id,
            String name,
            String couponNumber,
            String imageUrl,
            LocalDate expiryDate,
            GifticonCategory category,
            int displayOrder,
            boolean isAssigned
    ){
        public static AdminGifticonResponse of(Gifticon gifticon, String imageUrl){
            return AdminGifticonResponse.builder()
                    .id(gifticon.getId())
                    .name(gifticon.getName())
                    .couponNumber(gifticon.getCouponNumber())
                    .imageUrl(imageUrl)
                    .expiryDate(gifticon.getExpiryDate())
                    .category(gifticon.getCategory())
                    .displayOrder(gifticon.getDisplayOrder())
                    .isAssigned(gifticon.isAssigned())
                    .build();
        }
    }
}
