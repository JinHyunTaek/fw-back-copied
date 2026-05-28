package my.mma.api.bet.repository;

import my.mma.api.bet.entity.BetCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BetCardRepository extends JpaRepository<BetCard,Long> {
//    @Query("""
//            select sum(case when v.winnerId = :winnerId then 1 else 0 end) as winnerVotes,
//             sum(case when v.winnerId = :loserId then 1 else 0 end) as loserVotes
//             from Vote v where v.fighterFightEvent.id=:ffeId
//            """)
//    VoteCntDto countVoteByFfeIdAndFighters(
//            @Param("ffeId") Long ffeId, @Param("winnerId") Long winnerId, @Param("loserId") Long loserId
//    );
    @Modifying(clearAutomatically = true)
    @Query("delete from BetCard bc where bc.bet.user.id=:userId")
    void deleteBetCardsByUserId(@Param("userId") Long userId);

    @Query("select count(*) from BetCard bc inner join bc.bet b where b.user.id=:userId and b.fightEvent.id=:fightEventId")
    int countByUserIdAndFightEventId(@Param("userId") Long userId, @Param("fightEventId") Long fightEventId);



}
