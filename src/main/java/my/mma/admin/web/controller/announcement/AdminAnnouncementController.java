package my.mma.admin.web.controller.announcement;

import lombok.RequiredArgsConstructor;
import my.mma.api.announcement.dto.AdminAnnouncementDetailDto;
import my.mma.api.announcement.dto.AdminAnnouncementRequest;
import my.mma.api.announcement.service.AnnouncementService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.data.domain.Sort.Direction.DESC;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/htj-admin/announcement")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/announcements")
    public String announcements(
            @PageableDefault(sort = {"pinned", "createdDateTime"}, direction = DESC) Pageable pageable,
            Model model
    ) {
        model.addAttribute("announcements", announcementService.getAnnouncements(pageable));
        model.addAttribute("activeMenu", "announcement");
        return "admin/announcement/announcements";
    }

    @GetMapping("/saveForm")
    public String saveForm(Model model) {
        model.addAttribute("form", new AdminAnnouncementRequest(null, null, false));
        return "admin/announcement/saveForm";
    }

    @PostMapping("")
    public String save(
            @Validated @ModelAttribute("form") AdminAnnouncementRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("form", request);
            return "admin/announcement/saveForm";
        }
        Long id = announcementService.save(request);
        return "redirect:/htj-admin/announcement/" + id;
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Validated @ModelAttribute("form") AdminAnnouncementRequest updateRequest,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            AdminAnnouncementDetailDto detail = announcementService.detail(id);
            model.addAttribute("announcementId", detail.id());
            // 요청 값 유지
            model.addAttribute("form", updateRequest);
            return "admin/announcement/detail";
        }
        announcementService.updateAnnouncement(id, updateRequest);
        return "redirect:/htj-admin/announcement/{id}";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model
    ) {
        AdminAnnouncementDetailDto detail = announcementService.detail(id);
        model.addAttribute("announcementId", detail.id());
        model.addAttribute("form", new AdminAnnouncementRequest(
                detail.title(),
                detail.content(),
                detail.pinned()
        ));
        return "admin/announcement/detail";
    }

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable Long id
    ) {
        announcementService.delete(id);
        return "redirect:/htj-admin/announcement/announcements";
    }

}
