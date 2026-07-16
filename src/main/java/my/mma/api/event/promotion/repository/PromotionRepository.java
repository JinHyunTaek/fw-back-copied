package my.mma.api.event.promotion.repository;

import my.mma.api.event.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("select p from Promotion p where :today between p.startDate and p.endDate")
    List<Promotion> findActive(LocalDate today);

    @Query("select p from Promotion p" +
            " where p.startDate <= :today and p.endDate >= :from" +
            " order by p.startDate desc")
    List<Promotion> findRecent(@Param("today") LocalDate today, @Param("from") LocalDate from);

    // 추첨 대상: 발표일이 도래(오늘 포함, 밀린 것까지)했고 아직 추첨 안 된 프로모션
    List<Promotion> findByAnnounceDateAndDrawnAtIsNull(LocalDate today);

}
