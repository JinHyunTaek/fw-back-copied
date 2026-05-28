package my.mma.api.alert.dto;

import my.mma.api.alert.constant.AlertTarget;

public record UpdatePreferenceRequest(boolean on, AlertTarget alertTarget, boolean isWeeklyEvent) {
}
