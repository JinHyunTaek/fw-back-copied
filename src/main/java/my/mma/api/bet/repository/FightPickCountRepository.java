package my.mma.api.bet.repository;

import my.mma.api.bet.entity.FightPickCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface FightPickCountRepository extends JpaRepository<FightPickCount, Long> {

    @Modifying
    @Query("""
            update FightPickCount f
            set f.firstFighterPickCount = f.firstFighterPickCount + :delta
            where f.fighterFightEvent.id = :ffeId
            """)
    void updateFirstFighterPick(@Param("ffeId") Long ffeId, @Param("delta") int delta);

    @Modifying
    @Query("""
            update FightPickCount f
            set f.lastFighterPickCount = f.lastFighterPickCount + :delta
            where f.fighterFightEvent.id = :ffeId
            """)
    void updateLastFighterPick(@Param("ffeId") Long ffeId, @Param("delta") int delta);

    Set<FightPickCount> findByFighterFightEventIdIn(Set<Long> ffeIds);

    boolean existsByFighterFightEventIdIn(List<Long> ffeIds);

    @Query("""
            select f.fighterFightEvent.id
            from FightPickCount f
            where f.fighterFightEvent.id in :ffeIds
            """)
    Set<Long> findFfeIdsByFfeIds(@Param("ffeIds") List<Long> ffeIds);

}
