package my.mma.api.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import my.mma.api.inquiry.entity.constant.InquiryCategory;

public record InquirySaveRequest(@NotNull InquiryCategory category, @NotBlank String content) {
}
