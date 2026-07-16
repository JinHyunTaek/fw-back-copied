package my.mma.admin.web.promotion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.Promotion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public record AdminPromotionSaveRequest(
        @NotBlank String title,
        @NotBlank String benefit,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate announceDate,
        @Positive int maxWinnerCount,
        String notice,
        @NotEmpty @Valid List<AdminGifticonSaveRequest> gifticonSaveRequests
) {

    public Promotion toEntity(){
        return Promotion.builder()
                .title(title)
                .benefit(benefit)
                .startDate(startDate)
                .endDate(endDate)
                .announceDate(announceDate)
                .maxWinnerCount(maxWinnerCount)
                .notice(notice)
                .build();
    }

}
