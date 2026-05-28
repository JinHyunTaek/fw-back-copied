package my.mma.api.inquiry.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record InquiryBodyResponse(String content, String answer) {
}
