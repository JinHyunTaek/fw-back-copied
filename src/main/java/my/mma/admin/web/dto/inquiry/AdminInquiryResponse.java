package my.mma.admin.web.dto.inquiry;

import lombok.Builder;
import my.mma.api.inquiry.entity.constant.InquiryCategory;

import java.time.LocalDateTime;

@Builder
public record AdminInquiryResponse(long id, String nickname, InquiryCategory category, LocalDateTime createdAt) {
}
