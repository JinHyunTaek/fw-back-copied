package my.mma.admin.web.dto.user;

import my.mma.api.report.entity.ReportCategory;

public record AdminUserPunishmentRequest(boolean punish, ReportCategory reportCategory) {
}
