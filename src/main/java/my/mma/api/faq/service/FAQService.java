package my.mma.api.faq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.faq.dto.*;
import my.mma.api.faq.dto.FAQAnswersResponse.FAQAnswerResponse;
import my.mma.api.faq.entity.FAQ;
import my.mma.api.faq.entity.FAQCategory;
import my.mma.api.faq.repository.FAQRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class FAQService {

    private final FAQRepository faqRepository;

    @Cacheable(value = "faqs")
    public List<FAQResponse> getFaqs() {
        return faqRepository.getFAQListProjection();
    }

    // for admin
    public FAQDetailResponse detail(Long id) {
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        return FAQDetailResponse.toDto(faq);
    }

    // for user
    @Cacheable(value = "faq", key = "#p0")
    public FAQAnswersResponse getFaqDetailsFromCategory(FAQCategory category) {
        List<FAQAnswerResponse> faqAnswerResponses = faqRepository.findByFaqCategory(category)
                .stream().map(faq -> new FAQAnswerResponse(faq.getId(), faq.getAnswer())).collect(Collectors.toList());
        return new FAQAnswersResponse(faqAnswerResponses);

    }

    @CacheEvict(value = "faqs", allEntries = true)
    @Transactional
    public Long save(AdminFAQRequest request) {
        FAQ faq = faqRepository.save(request.toEntity());
        return faq.getId();
    }

    @Caching(evict = {
            @CacheEvict(value = "faqs", allEntries = true),
            @CacheEvict(value = "faq", key = "#request.faqCategory()"),
    })
    @Transactional
    public void update(Long id, AdminFAQRequest request) {
        FAQ faq = faqRepository.findById(id).orElseThrow(
                () -> new CustomException(ErrorCode.BAD_REQUEST_400));
        faq.update(request.question(), request.answer(), request.faqCategory());
    }

    @Caching(evict = {
            @CacheEvict(value = "faqs", allEntries = true),
            @CacheEvict(value = "faq", allEntries = true),
    })
    @Transactional
    public void delete(Long id) {
        faqRepository.deleteById(id);
    }

}
