package my.mma.api.faq.dto;

import java.util.List;

public record FAQAnswersResponse(List<FAQAnswerResponse> faqAnswers) {
    public record FAQAnswerResponse(Long id, String answer) {
    }
}
