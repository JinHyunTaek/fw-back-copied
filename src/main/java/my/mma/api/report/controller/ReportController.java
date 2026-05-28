package my.mma.api.report.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.report.service.ReportService;
import my.mma.api.report.dto.ReportRequest;
import my.mma.api.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("")
    public ResponseEntity<Boolean> report(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestBody ReportRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reportService.report(userDetails.getUsername(), request));
    }

}
