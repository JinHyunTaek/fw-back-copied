package my.mma.api.alert.dto;

import jakarta.validation.constraints.NotNull;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.alert.entity.Alert;
import my.mma.api.user.entity.User;

public record UpdateAlertRequest(@NotNull Long targetId, @NotNull Boolean on, @NotNull AlertTarget alertTarget) {
    public Alert toEntity(User user){
        return Alert.builder()
                .user(user)
                .targetId(targetId)
                .alertTarget(alertTarget)
                .build();
    }
}