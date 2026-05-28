package my.mma.api.faq.dto;

import my.mma.api.faq.entity.FAQCategory;

public record FAQResponse(Long id, String question, FAQCategory faqCategory) {
}
