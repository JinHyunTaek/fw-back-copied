package my.mma.admin.web.bet.controller;

import lombok.RequiredArgsConstructor;
import my.mma.admin.event.service.BetPointHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/htj-admin/unsettled-bet")
public class UnsettledBetProcessController {

    private final BetPointHandler betPointHandler;

    @PostMapping("")
    public String processUnsettledBets(){
        betPointHandler.retryUnsettledBets();
        return "redirect:/htj-admin/update-buttons";
    }

}
