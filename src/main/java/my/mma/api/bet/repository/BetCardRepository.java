package my.mma.api.bet.repository;

import my.mma.api.bet.entity.BetCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BetCardRepository extends JpaRepository<BetCard,Long> {
//    @Query("""
//            select sum(case when v.winnerId = :winnerId then 1 else 0 end) as winnerVotes,
//             sum(case when v.winnerId = :loserId then 1 else 0 end) as loserVotes
//             from Vote v where v.fighterFightEvent.id=:ffeId
//            """)
//    VoteCntDto countVoteByFfeIdAndFighters(
//            @Param("ffeId") Long ffeId, @Param("winnerId") Long winnerId, @Param("loserId") Long loserId
//    );
    // 서브쿼리로 작성해야 이식성이 보장됨. bc.bet.user.id 처럼 연관관계를 2단계로 타면
    // JPQL DELETE가 조인을 필요로 해, MySQLDialect가 MySQL 전용 multi-table delete
    @Modifying(clearAutomatically = true)
    @Query("delete from BetCard bc where bc.bet.id in (select b.id from Bet b where b.user.id = :userId)")
    void deleteBetCardsByUserId(@Param("userId") Long userId);

    @Query("select count(*) from BetCard bc inner join bc.bet b where b.user.id=:userId and b.fightEvent.id=:fightEventId")
    int countByUserIdAndFightEventId(@Param("userId") Long userId, @Param("fightEventId") Long fightEventId);

    @Query("select bc from BetCard bc join fetch bc.bet b join fetch b.user" +
            " where cast(bc.createdDateTime as LocalDate) between :startDate and :endDate")
    List<BetCard> findPromotionDurationBetCards(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("select count(*) from BetCard bc inner join bc.bet b" +
            " where b.user.id = :userId" +
            " and cast(bc.createdDateTime as LocalDate) between :startDate and :endDate")
    int countByUserIdAndDuration(@Param("userId") Long userId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

}
