package my.mma.admin.web.dto.user;

import lombok.Builder;
import my.mma.api.report.entity.ReportCategory;

@Builder
public record AdminUserBetPointUpdateRequest(int point, int earnedBetSucceedPoint) {
}
