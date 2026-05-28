package my.mma.api.alert.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.dto.UpdateAlertRequest;
import my.mma.api.alert.dto.UpdatePreferenceRequest;
import my.mma.api.alert.dto.UserPreferencesDto;
import my.mma.api.alert.service.AlertService;
import my.mma.api.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping("")
    public ResponseEntity<Void> updateSingleAlert(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Validated UpdateAlertRequest request
    ) {
        alertService.updateSingleAlert(userDetails.getUsername(), request);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesDto> preferences(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return ResponseEntity.ok().body(alertService.getUserPreferences(userDetails.getUsername()));
    }

    @PostMapping("/preferences")
    public ResponseEntity<Void> updateAllPreferences(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("isOn") String isOn
    ){
        alertService.updateAllPreferences(userDetails.getUsername(), Boolean.parseBoolean(isOn));
        return ResponseEntity.ok().body(null);
    }

    @PostMapping("/preference")
    public ResponseEntity<Void> updateSinglePreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Validated UpdatePreferenceRequest request
            ){
        alertService.updateSinglePreference(userDetails.getUsername(), request);
        return ResponseEntity.ok().body(null);
    }

}
