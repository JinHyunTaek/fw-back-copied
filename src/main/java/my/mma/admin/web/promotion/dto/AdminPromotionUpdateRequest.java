package my.mma.admin.web.promotion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import my.mma.api.event.promotion.entity.GifticonCategory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public record AdminPromotionUpdateRequest(
        @NotBlank String title,
        @NotBlank String benefit,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate announceDate,
        @Positive int maxWinnerCount,
        String notice,
        // 미배정 기프티콘만 수정 대상 → 비어 있을 수 있으므로 @NotEmpty 아님
        @Valid List<AdminGifticonUpdateRequest> gifticons
) {

    public record AdminGifticonUpdateRequest(
            @NotNull Long id,
            @NotBlank String name,
            @NotBlank String couponNumber,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @NotNull GifticonCategory category,
            @Positive int displayOrder,
            MultipartFile image
    ) {
    }

}
