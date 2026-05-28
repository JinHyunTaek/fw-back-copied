package my.mma.api.user.dto;

import my.mma.api.user.entity.WithdrawalReasonCategory;

public record WithdrawalReasonDto(WithdrawalReasonCategory category, String description) {
}
