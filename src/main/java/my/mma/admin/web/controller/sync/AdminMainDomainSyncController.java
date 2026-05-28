package my.mma.admin.web.controller.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.stream.event.service.AdminStreamPollingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/htj-admin/update-buttons")
@RequiredArgsConstructor
public class AdminMainDomainSyncController {

    private final AdminStreamPollingService adminStreamPollingService;

    @GetMapping("")
    public String home(Model model){
        model.addAttribute("activeMenu", "update-buttons");
        model.addAttribute("prePollingTime", adminStreamPollingService.getPrePollingScheduledTime());
        return "admin/update/main_domain_update";
    }

}
