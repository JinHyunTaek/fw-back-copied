package my.mma.admin.web.system.controller;

import lombok.RequiredArgsConstructor;
import my.mma.admin.web.system.dto.AdminAppStatusUpdateRequest;
import my.mma.admin.web.system.service.AdminAppStatusService;
import my.mma.api.status.dto.AppStatusResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/htj-admin/system/app-status")
public class AdminAppStatusController {

    private final AdminAppStatusService appStatusService;

    @GetMapping("")
    public String appStatus(Model model){
        setAppStatusModel(model);
        return "admin/system/app-status";
    }

    @PostMapping("")
    public String update(
            @Validated @ModelAttribute AdminAppStatusUpdateRequest request,
            BindingResult bindingResult,
            Model model
    ){
        if(bindingResult.hasErrors()){
            setAppStatusModel(model);
            return "admin/system/app-status";
        }
        appStatusService.update(request);
        return "redirect:/htj-admin/system/app-status";
    }

    private void setAppStatusModel(Model model) {
        AppStatusResponse appStatus = appStatusService.getAppStatusResponse();
        model.addAttribute("appStatus", appStatus);
        model.addAttribute("form", AdminAppStatusUpdateRequest.of(appStatus));
        model.addAttribute("activeMenu", "app-status");
    }

}
