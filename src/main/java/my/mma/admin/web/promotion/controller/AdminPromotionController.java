package my.mma.admin.web.promotion.controller;

import lombok.RequiredArgsConstructor;
import my.mma.admin.web.promotion.dto.AdminGifticonSaveRequest;
import my.mma.admin.web.promotion.dto.AdminPromotionDetailResponse;
import my.mma.admin.web.promotion.dto.AdminPromotionDetailResponse.AdminGifticonResponse;
import my.mma.admin.web.promotion.dto.AdminPromotionSaveRequest;
import my.mma.admin.web.promotion.dto.AdminPromotionUpdateRequest;
import my.mma.admin.web.promotion.dto.AdminPromotionUpdateRequest.AdminGifticonUpdateRequest;
import my.mma.admin.web.promotion.service.AdminPromotionService;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/htj-admin/promotion")
public class AdminPromotionController {

    private final AdminPromotionService adminPromotionService;

    @GetMapping("")
    public String promotionDashboard(
            @PageableDefault Pageable pageable,
            Model model
    ) {
        model.addAttribute("promotions", adminPromotionService.get(pageable));
        model.addAttribute("activeMenu", "promotion");
        return "admin/promotion/dashboard";
    }

    @GetMapping("/saveForm")
    public String saveForm(Model model) {
        model.addAttribute("form", new AdminPromotionSaveRequest(
                null, null, null, null, null, 0, null, new ArrayList<>()));
        model.addAttribute("activeMenu", "promotion");
        return "admin/promotion/saveForm";
    }

    @PostMapping("")
    public String save(
            @Validated @ModelAttribute("form") AdminPromotionSaveRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "promotion");
            return "admin/promotion/saveForm";
        }
        try {
            adminPromotionService.save(request);
        } catch (CustomException e) {
            bindingResult.reject(e.getErrorCode().name(), businessMessage(e.getErrorCode()));
            model.addAttribute("activeMenu", "promotion");
            return "admin/promotion/saveForm";
        }
        return "redirect:/htj-admin/promotion";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model
    ) {
        model.addAttribute("promotion", adminPromotionService.detail(id));
        model.addAttribute("activeMenu", "promotion");
        return "admin/promotion/detail";
    }

    @GetMapping("/{id}/editForm")
    public String editForm(
            @PathVariable Long id,
            Model model
    ) {
        AdminPromotionDetailResponse detail = adminPromotionService.detail(id);

        // 배정된 기프티콘은 수정 불가 → 미배정만 폼에 채움 (image는 변경 시에만 첨부 → null)
        List<AdminGifticonUpdateRequest> editable = detail.gifticons().stream()
                .filter(g -> !g.isAssigned())
                .map(g -> new AdminGifticonUpdateRequest(g.id(), g.name(), g.couponNumber(), g.expiryDate(), g.category(), g.displayOrder(), null))
                .toList();

        model.addAttribute("form", new AdminPromotionUpdateRequest(
                detail.title(), detail.benefit(), detail.startDate(), detail.endDate(),
                detail.announceDate(), detail.maxWinnerCount(), detail.notice(), editable));
        addEditViewModel(detail, model);
        return "admin/promotion/editForm";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Validated @ModelAttribute("form") AdminPromotionUpdateRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            addEditViewModel(adminPromotionService.detail(id), model);
            return "admin/promotion/editForm";
        }
        try {
            adminPromotionService.update(id, request);
        } catch (CustomException e) {
            bindingResult.reject(e.getErrorCode().name(), businessMessage(e.getErrorCode()));
            addEditViewModel(adminPromotionService.detail(id), model);
            return "admin/promotion/editForm";
        }
        return "redirect:/htj-admin/promotion/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        adminPromotionService.delete(id);
        return "redirect:/htj-admin/promotion";
    }

    // 기프티콘 1개 추가 (PathVariable id = promotionId)
    @PostMapping("/{id}/gifticon")
    public String saveGifticon(@PathVariable Long id,
                               @Validated @ModelAttribute AdminGifticonSaveRequest request,
                               BindingResult bindingResult,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "기프티콘 입력값을 확인해주세요. (상품명·쿠폰번호·유효기간·이미지 모두 필수)");
            return "redirect:/htj-admin/promotion/" + id;
        }
        try {
            adminPromotionService.saveGifticon(id, request);
            ra.addFlashAttribute("message", "기프티콘을 추가했습니다.");
        } catch (CustomException e) {
            ra.addFlashAttribute("error", businessMessage(e.getErrorCode()));
        }
        return "redirect:/htj-admin/promotion/" + id;
    }

    // 기프티콘 1개 삭제 (promotionId를 경로에 담아 리다이렉트를 단순화)
    @PostMapping("/{promotionId}/gifticon/{gifticonId}/delete")
    public String deleteGifticon(@PathVariable Long promotionId,
                                 @PathVariable Long gifticonId,
                                 RedirectAttributes ra) {
        try {
            adminPromotionService.deleteGifticon(promotionId, gifticonId);
            ra.addFlashAttribute("message", "기프티콘을 삭제했습니다.");
        } catch (CustomException e) {
            ra.addFlashAttribute("error", businessMessage(e.getErrorCode()));
        }
        return "redirect:/htj-admin/promotion/" + promotionId;
    }

    private String businessMessage(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_EXPIRY_DATE_400 -> "기프티콘 유효기간은 당첨자 발표일로부터 최소 10일 이후여야 합니다. 유효기간을 확인 후 다시 등록해주세요.";
            case PROMOTION_ALREADY_DRAWN_400 -> "이미 추첨이 완료된 프로모션은 수정·삭제하거나 기프티콘을 추가/삭제할 수 없습니다.";
            case BAD_REQUEST_400 -> "입력값을 확인해주세요. (기프티콘 수가 최대 당첨자 수와 같아야 하며, 배정 완료된 기프티콘은 수정할 수 없습니다.)";
            default -> "요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.";
        };
    }

    private void addEditViewModel(AdminPromotionDetailResponse detail, Model model) {
        model.addAttribute("promotionId", detail.id());
        model.addAttribute("assignedGifticons", detail.gifticons().stream()
                .filter(AdminGifticonResponse::isAssigned).toList());

        Map<Long, String> gifticonImageUrls = new HashMap<>();
        detail.gifticons().forEach(g -> gifticonImageUrls.put(g.id(), g.imageUrl()));
        model.addAttribute("gifticonImageUrls", gifticonImageUrls);

        model.addAttribute("activeMenu", "promotion");
    }

}
