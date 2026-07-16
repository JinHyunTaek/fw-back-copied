package my.mma.api.event.promotion.repository;

import my.mma.api.event.promotion.entity.PromotionWinner;
import my.mma.api.event.promotion.entity.SendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromotionWinnerRepository extends JpaRepository<PromotionWinner, Long> {

    // 상세 화면 당첨자 렌더링: 닉네임·기프티콘명을 기프티콘 우선순위(displayOrder) 오름차순으로
    @Query("select pw from PromotionWinner pw " +
            "join fetch pw.winner join fetch pw.gifticon g " +
            "where pw.promotion.id = :id " +
            "order by g.displayOrder asc")
    List<PromotionWinner> findWinnerGifticonInfoByPromotionId(Long id);

    // 관리자 대시보드: 당첨자 목록 (유저·기프티콘 한 번에)
    @Query("select pw from PromotionWinner pw " +
            "join fetch pw.winner join fetch pw.gifticon " +
            "where pw.promotion.id = :promotionId")
    List<PromotionWinner> findForAdminByPromotionId(@Param("promotionId") Long promotionId);

    // 일괄 발송 대상: 미발송(PENDING/FAILED) 당첨자 ID만 조회.
    // 실제 엔티티 로딩·발송은 GifticonMailSender.send(id)가 건별 트랜잭션에서 재조회하므로 페치 조인 불필요.
    @Query("select pw.id from PromotionWinner pw " +
            "where pw.promotion.id = :promotionId and pw.sendStatus <> :sent")
    List<Long> findUnsentWinnerIdsByPromotionId(@Param("promotionId") Long promotionId,
                                                @Param("sent") SendStatus sent);
}
