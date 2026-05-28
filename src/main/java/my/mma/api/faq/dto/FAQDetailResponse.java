package my.mma.api.faq.dto;

import my.mma.api.faq.entity.FAQ;
import my.mma.api.faq.entity.FAQCategory;

public record FAQDetailResponse(Long id, String question, String answer, FAQCategory faqCategory) {
    public static FAQDetailResponse toDto(FAQ faq){
        return new FAQDetailResponse(faq.getId(), faq.getQuestion(), faq.getAnswer(), faq.getFaqCategory());
    }
}
