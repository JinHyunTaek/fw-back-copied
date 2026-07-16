package my.mma.admin.web.event.controller;

import lombok.RequiredArgsConstructor;
import my.mma.admin.stream.event.service.AdminStreamPollingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/htj-admin/event/save_upcoming")
public class SaveUpcomingEventController {

    private final AdminStreamPollingService adminStreamPollingService;

    @PostMapping("")
    public String saveUpcomingEvents(){
        adminStreamPollingService.syncAllAndSchedule();
        return "redirect:/htj-admin/update-buttons";
    }

}
