package my.mma.admin.web.promotion.repository;

import my.mma.admin.web.promotion.dto.AdminPromotionResponse;
import my.mma.api.event.promotion.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminPromotionRepository extends JpaRepository<Promotion, Long> {
    @Query("select new my.mma.admin.web.promotion.dto.AdminPromotionResponse" +
            "(p.id,p.title, p.startDate,p.endDate,p.announceDate, cast(p.createdDateTime as LocalDate))" +
            " from Promotion p order by p.id desc")
    Page<AdminPromotionResponse> getPromotions(Pageable pageable);
}
