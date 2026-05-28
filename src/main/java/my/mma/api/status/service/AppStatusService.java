package my.mma.api.status.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.status.controller.AppStatusQueryService;
import my.mma.api.status.dto.AppStatusResponse;
import my.mma.api.status.entity.AppStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppStatusService {

    private final AppStatusQueryService appStatusQueryService;

    public AppStatusResponse getAppStatus() {
        AppStatus appStatus = appStatusQueryService.getAppStatus();
        return AppStatusResponse.of(appStatus.getMinVersion(), appStatus.getLatestVersion());
    }

}
