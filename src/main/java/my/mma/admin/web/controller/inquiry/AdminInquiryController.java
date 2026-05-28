package my.mma.admin.web.controller.inquiry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.dto.inquiry.AdminInquiryAnswerRequest;
import my.mma.admin.web.dto.inquiry.AdminInquiryDetailResponse;
import my.mma.admin.web.dto.inquiry.AdminInquiryResponse;
import my.mma.admin.web.service.inquiry.AdminInquiryService;
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
@RequestMapping("/htj-admin/inquiry")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @GetMapping("/all")
    public String inquiries(
            @PageableDefault() Pageable pageable,
            Model model
    ) {
        Page<AdminInquiryResponse> inquiries = adminInquiryService.inquiries(pageable);
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("activeMenu", "inquiry");
        return "admin/inquiry/inquiries";
    }

    @GetMapping("/{id}")
    public String inquiryDetail(@PathVariable("id") Long inquiryId, Model model){
        AdminInquiryDetailResponse inquiry = adminInquiryService.inquiryDetail(inquiryId);
        model.addAttribute("inquiry",inquiry);
        model.addAttribute("form", new AdminInquiryAnswerRequest(inquiryId, null));
        return "admin/inquiry/inquiry_detail";
    }

    @PostMapping("")
    public String updateAnswer(@Validated @ModelAttribute("form") AdminInquiryAnswerRequest request,
                               BindingResult bindingResult,
                               Model model){
        if(bindingResult.hasErrors()){
            AdminInquiryDetailResponse response = adminInquiryService.inquiryDetail(request.id());
            model.addAttribute("inquiry", response);
            return "admin/inquiry/inquiry_detail";
        }
        adminInquiryService.updateAnswer(request);
        return "redirect:/htj-admin/inquiry/all";
    }

}
