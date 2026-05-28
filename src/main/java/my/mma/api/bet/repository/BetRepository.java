package my.mma.api.bet.repository;

import my.mma.api.bet.entity.Bet;
import my.mma.api.user.dto.UserBetRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BetRepository extends JpaRepository<Bet, Long> {

    @EntityGraph(attributePaths = {
            "betCards",
            "betCards.fighterFightEvent",
            "betCards.fighterFightEvent.winner",
            "betCards.fighterFightEvent.loser"
    })
    List<Bet> findByFightEventIdAndUserId(@Param("eventId") long eventId, @Param("userId") long userId);

    @Query("select distinct b from Bet b join fetch b.betCards bc join fetch bc.fighterFightEvent ffe join fetch ffe.winner join fetch b.user where b.fightEvent.id=:eventId")
    List<Bet> findByEventIdWithBetCards(@Param("eventId") long eventId);

    @Query("select distinct b from Bet b join fetch b.betCards bc join fetch bc.fighterFightEvent ffe join fetch ffe.winner join fetch b.user where b.settled = false")
    List<Bet> findAllUnsettledWithDetails();

    @Query("""
            select coalesce(sum(case when b.succeed = true then 1 else 0 end),0) as win,
             coalesce(sum(case when b.succeed = false then 1 else 0 end),0) as loss,
             coalesce(sum(case when b.succeed is null then 1 else 0 end),0) as noContest
             from Bet b where b.user.id=:userId and b.fightEvent.completed = true
            """)
    UserBetRecord getUserBetRecord(@Param("userId") Long userId);

    //    @EntityGraph(attributePaths = {
//            "fightEvent",
//            "betCards",
//            "betCards.fighterFightEvent",
//            "betCards.fighterFightEvent.winner",
//            "betCards.fighterFightEvent.loser"
//    })
    @Query("select distinct b from Bet b" +
            " join fetch b.fightEvent" +
            " join fetch b.betCards bc" +
            " join fetch bc.fighterFightEvent ffe" +
            " join fetch ffe.winner" +
            " join fetch ffe.loser" +
            " where b.id in :ids")
    List<Bet> findWithDetails(@Param("ids") List<Long> ids);

    // for finding recent ranker's bets (recent 3 event ids that user made bet (completed event))
    @Query("""
            select distinct b.fightEvent.id from Bet b
            where b.user.id = :userId and b.fightEvent.completed = true
            group by b.fightEvent.id
            order by max(b.fightEvent.displayDate) desc
            """)
    List<Long> findRecentCompletedEventIdsUserBetted(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select b.id from Bet b
            where b.user.id = :userId and b.fightEvent.id in :eventIds
            order by b.createdDateTime desc
            """)
    List<Long> findBetIdsByUserIdAndEventIds(@Param("userId") Long userId, @Param("eventIds") List<Long> eventIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from Bet b where b.user.id=:userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
