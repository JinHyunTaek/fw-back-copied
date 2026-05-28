package my.mma.api.faq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import my.mma.api.faq.entity.FAQ;
import my.mma.api.faq.entity.FAQCategory;

public record AdminFAQRequest(@NotBlank @Size(min = 5) String question,
                              @NotBlank @Size(min = 10) String answer,
                              @NotNull FAQCategory faqCategory) {
    public FAQ toEntity(){
        return FAQ.builder()
                .question(question)
                .answer(answer)
                .faqCategory(faqCategory)
                .build();
    }
}
