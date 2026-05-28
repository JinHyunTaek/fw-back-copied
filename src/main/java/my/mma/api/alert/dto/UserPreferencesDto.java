package my.mma.api.alert.dto;

public record UserPreferencesDto(
        boolean fighterAlertEnabled,
        boolean eventAlertEnabled,
        boolean weeklyEventAlertEnabled
) {}
