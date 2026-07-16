package my.mma.admin.web.report.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.report.dto.AdminActivateReportRequest;
import my.mma.admin.web.report.dto.AdminReportResponse;
import my.mma.admin.web.report.service.AdminReportService;
import my.mma.api.report.entity.ReportCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
@Slf4j
@RequestMapping("/htj-admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/all")
    public String reports(
            @PageableDefault Pageable pageable,
            Model model
    ) {
        Page<AdminReportResponse> reports = adminReportService.getReports(pageable);
        addReportsPageAttributes(model, reports);
        return "admin/report/reports";
    }

    @PostMapping("")
    public String activate(
            @Validated @ModelAttribute("form") AdminActivateReportRequest request,
            BindingResult bindingResult,
            Model model,
            @PageableDefault Pageable pageable
    ){
        if(bindingResult.hasErrors()){
            Page<AdminReportResponse> reports = adminReportService.getReports(pageable);
            addReportsPageAttributes(model, reports);
            return "admin/report/reports";
        }
        adminReportService.activateReport(request);
        return "redirect:/htj-admin/report/all";
    }

    @DeleteMapping("")
    public String deactivate(
            @RequestParam String messageId
    ){
        adminReportService.deactivateReport(messageId);
        return "redirect:/htj-admin/report/all";
    }

    private static void addReportsPageAttributes(Model model, Page<AdminReportResponse> reports) {
        model.addAttribute("reports", reports);
        model.addAttribute("activeMenu", "report");
        model.addAttribute("categories", ReportCategory.values());
    }

}
