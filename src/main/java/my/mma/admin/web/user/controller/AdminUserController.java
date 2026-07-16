package my.mma.admin.web.user.controller;

import lombok.RequiredArgsConstructor;
import my.mma.admin.web.user.dto.AdminUserBetPointUpdateRequest;
import my.mma.admin.web.user.dto.AdminUserDetailResponse;
import my.mma.admin.web.user.dto.AdminUserPunishmentRequest;
import my.mma.admin.web.user.service.AdminUserService;
import my.mma.api.bet.service.UserRecentBetHistoryService;
import my.mma.api.report.entity.ReportCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/htj-admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserRecentBetHistoryService betHistoryService;

    @GetMapping("/users")
    public String users(
            @PageableDefault Pageable pageable,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "betUnsettled", required = false) Boolean betUnsettled,
            Model model
    ) {
        model.addAttribute("page", adminUserService.users(pageable, nickname, betUnsettled));
        model.addAttribute("activeMenu", "user");
        return "admin/user/users";
    }

    @GetMapping("/{id}")
    public String user(
            @PathVariable Long id,
            @RequestParam(value = "tab", defaultValue = "info") String tab,
            Model model
    ) {
        AdminUserDetailResponse user = adminUserService.detail(id);
        if ("bet".equals(tab))
            model.addAttribute("bets", betHistoryService.userBetHistory(id));
        model.addAttribute("user", user);
        model.addAttribute("betForm", new AdminUserBetPointUpdateRequest(
                user.point(), user.earnedBetSucceedPoint()));
        model.addAttribute("reportCategories", ReportCategory.values());
        model.addAttribute("reportForm", new AdminUserPunishmentRequest(user.punished(), null));
        return "admin/user/detail";
    }

    @PostMapping("/betPoint/{id}")
    public String updateBetPoint(
            @PathVariable("id") Long id,
            @ModelAttribute("betForm") AdminUserBetPointUpdateRequest request
    ) {
        adminUserService.updatePoint(id, request);
        return "redirect:/htj-admin/user/{id}";
    }

    @PostMapping("/banState/{id}")
    public String updatePunishmentState(
            @PathVariable("id") Long id,
            @ModelAttribute("reportForm") AdminUserPunishmentRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (request.punish() && request.reportCategory() == null) {
            bindingResult.rejectValue(
                    "reportCategory",
                    "required",
                    "징계 카테고리를 선택해주세요."
            );
            AdminUserDetailResponse user = adminUserService.detail(id);
            model.addAttribute("reportForm", request);
            model.addAttribute("user", user);
            model.addAttribute("betForm", new AdminUserBetPointUpdateRequest(
                    user.point(), user.earnedBetSucceedPoint()));
            model.addAttribute("reportCategories", ReportCategory.values());
            return "admin/user/detail";
        }
        adminUserService.updatePunishmentState(id, request);
        return "redirect:/htj-admin/user/{id}";
    }

}
