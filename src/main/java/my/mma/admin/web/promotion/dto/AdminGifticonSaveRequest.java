package my.mma.admin.web.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.GifticonCategory;
import my.mma.api.event.promotion.entity.Promotion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record AdminGifticonSaveRequest(
        @NotBlank String name,
        @NotBlank String couponNumber,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
        @NotNull GifticonCategory category,
        @Positive int displayOrder,
        MultipartFile image
) {
    public Gifticon toEntity(Promotion promotion, String key){
        return Gifticon.builder()
                .name(name)
                .couponNumber(couponNumber)
                .expiryDate(expiryDate)
                .category(category)
                .displayOrder(displayOrder)
                .promotion(promotion)
                .imageKey(key)
                .build();
    }
}
