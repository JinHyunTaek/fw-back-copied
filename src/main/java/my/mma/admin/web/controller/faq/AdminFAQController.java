package my.mma.admin.web.controller.faq;

import lombok.RequiredArgsConstructor;
import my.mma.api.faq.dto.AdminFAQRequest;
import my.mma.api.faq.dto.FAQDetailResponse;
import my.mma.api.faq.entity.FAQCategory;
import my.mma.api.faq.service.FAQService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@RequestMapping("/htj-admin/faq")
@RequiredArgsConstructor
public class AdminFAQController {

    private final FAQService faqService;

    @GetMapping("/saveForm")
    public String saveForm(Model model){
        model.addAttribute("form", new AdminFAQRequest(null,null,null));
        model.addAttribute("categories", FAQCategory.values());
        return "admin/faq/saveForm";
    }

    @GetMapping("/faqs")
    public String faqs(Model model){
        model.addAttribute("faqs", faqService.getFaqs());
        model.addAttribute("activeMenu", "faq");
        return "admin/faq/faqs";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable("id") Long id,
            Model model){
        FAQDetailResponse detail = faqService.detail(id);
        model.addAttribute("faqId",detail.id());
        model.addAttribute("categories", FAQCategory.values());
        model.addAttribute("form", new AdminFAQRequest(detail.question(),detail.answer(),detail.faqCategory()));
        return "admin/faq/detail";
    }

    @PostMapping("")
    public String save(
            @Validated @ModelAttribute("form") AdminFAQRequest request,
            BindingResult bindingResult,
            Model model
    ){
        if(bindingResult.hasErrors()){
            model.addAttribute("form", request);
            model.addAttribute("categories", FAQCategory.values());
            return "admin/faq/saveForm";
        }
        Long id = faqService.save(request);
        return "redirect:/htj-admin/faq/"+id;
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable("id") Long id,
            @Validated @ModelAttribute("form") AdminFAQRequest request,
            BindingResult bindingResult,
            Model model
    ){
        if(bindingResult.hasErrors()){
            FAQDetailResponse detail = faqService.detail(id);
            model.addAttribute("faqId",detail.id());
            model.addAttribute("categories", FAQCategory.values());
            // 요청 값 유지
            model.addAttribute("form", request);
            return "admin/faq/detail";
        }
        faqService.update(id, request);
        return "redirect:/htj-admin/faq/{id}";
    }

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable("id") Long id
    ){
        faqService.delete(id);
        return "redirect:/htj-admin/faq/faqs";
    }

}
