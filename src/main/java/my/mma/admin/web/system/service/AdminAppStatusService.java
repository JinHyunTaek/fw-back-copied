package my.mma.admin.web.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.system.dto.AdminAppStatusUpdateRequest;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.status.controller.AppStatusQueryService;
import my.mma.api.status.dto.AppStatusResponse;
import my.mma.api.status.entity.AppStatus;
import my.mma.api.status.repository.AppStatusRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAppStatusService {

    private final AppStatusQueryService appStatusQueryService;
    private final AppStatusRepository appStatusRepository;

    public AppStatusResponse getAppStatusResponse() {
        AppStatus appStatus = appStatusQueryService.getAppStatus();
        return AppStatusResponse.of(appStatus.getMinVersion(), appStatus.getLatestVersion());
    }

    @CacheEvict(value = "appStatus", allEntries = true)
    @Loggable
    @Transactional
    public void update(AdminAppStatusUpdateRequest request) {
        AppStatus appStatus = appStatusRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("AppStatus not found"));
        appStatus.update(request.minVersion(), request.latestVersion());
    }

}
