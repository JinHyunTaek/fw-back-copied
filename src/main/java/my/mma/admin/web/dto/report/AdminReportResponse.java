package my.mma.admin.web.dto.report;

import lombok.Builder;
import my.mma.api.report.entity.ReportCategory;

import java.time.LocalDate;

@Builder
public record AdminReportResponse(String id,
                                  long reportedUserId,
                                  int reportedCount,
                                  String reportedUserNickname,
                                  ReportCategory reportCategory,
                                  String messageSnapshot,
                                  LocalDate reportedDate
) {
}
