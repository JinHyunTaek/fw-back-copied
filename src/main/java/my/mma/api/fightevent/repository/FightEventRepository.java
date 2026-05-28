package my.mma.api.fightevent.repository;

import my.mma.api.fightevent.entity.FightEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FightEventRepository extends JpaRepository<FightEvent, Long> {
    @EntityGraph(attributePaths = {
            "fighterFightEvents",
            "fighterFightEvents.winner",
            "fighterFightEvents.loser"
    })
    List<FightEvent> findByCompletedIsFalse();

    @Query("select fe.displayDate from FightEvent fe where fe.displayDate between :start and :end")
    List<LocalDate> findDisplayDatesByYearMonth(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select distinct fe from FightEvent fe join fetch fe.fighterFightEvents ffe" +
            " join fetch ffe.winner" +
            " join fetch ffe.loser" +
            " where ffe.canceled=false and" +
            " fe.displayDate= :date" +
            " order by ffe.cardOrder")
    List<FightEvent> findByDisplayDateWithFightsNotCanceled(@Param("date") LocalDate date);

    @EntityGraph(attributePaths = {
            "fighterFightEvents",
            "fighterFightEvents.winner",
            "fighterFightEvents.loser"
    })
    FightEvent findFirstByCompletedIsFalseOrderByEventDateAsc();

    Optional<Page<FightEvent>> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("select distinct fe from FightEvent fe join fetch fe.fighterFightEvents ffe join fetch ffe.winner" +
            " join fetch ffe.loser where fe.id=:id")
    Optional<FightEvent> findByIdWithFfes(@Param("id") Long id);

}
