package my.mma.admin.web.dto.system;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import my.mma.api.status.dto.AppStatusResponse;

public record AdminAppStatusUpdateRequest(
        @NotNull
        @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$")
        String minVersion,
        @NotNull
        @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$")
        String latestVersion) {

    public static AdminAppStatusUpdateRequest of(AppStatusResponse appStatus){
        return new AdminAppStatusUpdateRequest(appStatus.minVersion(), appStatus.latestVersion());
    }

}
