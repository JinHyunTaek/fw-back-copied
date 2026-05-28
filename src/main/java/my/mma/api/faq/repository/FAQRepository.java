package my.mma.api.faq.repository;

import my.mma.api.faq.dto.FAQResponse;
import my.mma.api.faq.entity.FAQ;
import my.mma.api.faq.entity.FAQCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FAQRepository extends JpaRepository<FAQ, Long> {

    @Query("select new my.mma.api.faq.dto.FAQResponse(f.id, f.question,f.faqCategory) from FAQ f")
    List<FAQResponse> getFAQListProjection();
    List<FAQ> findByFaqCategory(FAQCategory category);
}
