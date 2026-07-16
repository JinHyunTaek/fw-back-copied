package my.mma.admin.web.user.dto;

import lombok.Builder;
import my.mma.api.report.entity.ReportCategory;

@Builder
public record AdminUserBetPointUpdateRequest(int point, int earnedBetSucceedPoint) {
}
