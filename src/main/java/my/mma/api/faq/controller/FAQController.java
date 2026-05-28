package my.mma.api.faq.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.faq.dto.FAQAnswersResponse;
import my.mma.api.faq.dto.FAQResponse;
import my.mma.api.faq.entity.FAQCategory;
import my.mma.api.faq.service.FAQService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/faq")
public class FAQController {

    private final FAQService faqService;

    @GetMapping("/faqs")
    public ResponseEntity<List<FAQResponse>> faqs() {
        return ResponseEntity.ok().body(faqService.getFaqs());
    }

    @GetMapping("")
    public ResponseEntity<FAQAnswersResponse> faq(
            @RequestParam("category") FAQCategory faqCategory
    ) {
        return ResponseEntity.ok().body(faqService.getFaqDetailsFromCategory(faqCategory));
    }

}