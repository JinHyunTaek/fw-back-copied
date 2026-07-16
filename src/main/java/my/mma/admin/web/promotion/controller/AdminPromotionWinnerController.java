package my.mma.admin.web.promotion.controller;

import lombok.RequiredArgsConstructor;
import my.mma.admin.web.promotion.service.AdminPromotionWinnerService;
import my.mma.admin.web.promotion.service.GifticonMailSender;
import my.mma.api.event.promotion.service.PromotionDrawService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/htj-admin/promotion")
public class AdminPromotionWinnerController {

    private final AdminPromotionWinnerService winnerService;
    private final GifticonMailSender gifticonMailSender;
    private final PromotionDrawService promotionDrawService;

    /**
     * [임시/E2E] 자정 크론을 기다리지 않고 즉시 추첨한다.
     * 실제 운영 추첨은 {@code PromotionDrawScheduler}(자정)만 담당 — 이 엔드포인트는 배포 전 제거할 것.
     */
    @PostMapping("/{id}/draw")
    public String drawNow(@PathVariable Long id, RedirectAttributes ra) {
        promotionDrawService.draw(id);
        ra.addFlashAttribute("message", "추첨을 실행했습니다. (임시 트리거)");
        return "redirect:/htj-admin/promotion/" + id;
    }

    /** 당첨자 확인 대시보드 */
    @GetMapping("/{id}/winners")
    public String winners(@PathVariable Long id, Model model) {
        model.addAttribute("winners", winnerService.getWinners(id));
        model.addAttribute("promotionId", id);
        model.addAttribute("activeMenu", "promotion");
        return "admin/promotion/winners";
    }

    /** 개별 발송 */
    @PostMapping("/{promotionId}/winners/{winnerId}/send")
    public String send(@PathVariable Long promotionId, @PathVariable Long winnerId,
                       RedirectAttributes ra) {
        gifticonMailSender.send(winnerId);
        ra.addFlashAttribute("message", "발송 처리했습니다.");
        return "redirect:/htj-admin/promotion/" + promotionId + "/winners";
    }

    /** 미발송 일괄 발송 */
    @PostMapping("/{promotionId}/winners/send-all")
    public String sendAll(@PathVariable Long promotionId, RedirectAttributes ra) {
        winnerService.sendAll(promotionId);
        ra.addFlashAttribute("message", "미발송 당첨자를 일괄 발송 처리했습니다.");
        return "redirect:/htj-admin/promotion/" + promotionId + "/winners";
    }
}
