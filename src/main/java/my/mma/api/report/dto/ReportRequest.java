package my.mma.api.report.dto;

import my.mma.api.report.entity.ReportCategory;
import my.mma.api.user.entity.User;

public record ReportRequest(ReportCategory reportCategory, String messageId,
                            String messageSnapshot, long reportedUserId) {

}
