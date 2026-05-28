package my.mma.api.status.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.security.CustomUserDetails;
import my.mma.api.status.service.AppStatusService;
import my.mma.api.status.dto.AppStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-status")
public class AppStatusController {

    private final AppStatusService appStatusService;

    @GetMapping("")
    public ResponseEntity<AppStatusResponse> getAppStatus(
    ) {
        return ResponseEntity.ok().body(appStatusService.getAppStatus());
    }

}
