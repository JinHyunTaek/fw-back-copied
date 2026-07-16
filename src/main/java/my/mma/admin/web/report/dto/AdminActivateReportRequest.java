package my.mma.admin.web.report.dto;

import jakarta.validation.constraints.NotNull;
import my.mma.api.report.entity.ReportCategory;

public record AdminActivateReportRequest(@NotNull String messageId,
                                         @NotNull(message = "징계 카테고리를 선택해주세요.") ReportCategory category) {
}
