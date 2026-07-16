package my.mma.admin.web.inquiry.dto;

import jakarta.validation.constraints.NotNull;

public record AdminInquiryAnswerRequest(@NotNull Long id, @NotNull(message = "답변을 입력해주세요.") String answer) {
}
