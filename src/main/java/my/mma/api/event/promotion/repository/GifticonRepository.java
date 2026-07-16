package my.mma.api.event.promotion.repository;

import my.mma.api.event.promotion.entity.Gifticon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GifticonRepository extends JpaRepository<Gifticon, Long> {

    // 상세/삭제용: 전체 기프티콘(배정+미배정)을 등수(displayOrder) 순으로
    List<Gifticon> findByPromotionIdOrderByDisplayOrderAsc(Long promotionId);

    void deleteByPromotionId(Long id);

}
