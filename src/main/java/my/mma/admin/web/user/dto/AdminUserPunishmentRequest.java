package my.mma.admin.web.user.dto;

import my.mma.api.report.entity.ReportCategory;

public record AdminUserPunishmentRequest(boolean punish, ReportCategory reportCategory) {
}
