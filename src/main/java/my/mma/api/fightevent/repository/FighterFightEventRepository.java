package my.mma.api.fightevent.repository;

import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.WinMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface FighterFightEventRepository extends JpaRepository<FighterFightEvent,Long> {
    // 해당 fighter의 특정 년도 fighterFightEvent(previous & upcoming) 정보 불러옴
     @Query("select ffe from FighterFightEvent ffe join fetch ffe.fightEvent" +
             " join fetch ffe.winner join fetch ffe.loser" +
             " where (ffe.loser.id=:fighterId or ffe.winner.id=:fighterId) and ffe.canceled=false" +
             " and function('YEAR',ffe.fightEvent.displayDate)=:year" +
             " order by ffe.fightEvent.eventDate desc")
    List<FighterFightEvent> findByFighterIdAndYear(@Param("fighterId") Long fighterId, @Param("year") int year);

    @Query("select ffe from FighterFightEvent ffe join fetch ffe.fightEvent" +
            " join fetch ffe.winner join fetch ffe.loser" +
            " where (ffe.loser.name=:fighterName or ffe.winner.name=:fighterName) and ffe.canceled=false")
    List<FighterFightEvent> findByFighterName(@Param("fighterName") String fighterName);

    @Query("select ffe from FighterFightEvent ffe join fetch ffe.fightEvent" +
            " join fetch ffe.winner join fetch ffe.loser" +
            " where (ffe.loser.id=:fighterId or ffe.winner.id=:fighterId) and ffe.canceled=false")
    List<FighterFightEvent> findByFighterId(@Param("fighterId") Long fighterId);

    @Query("select ffe from FighterFightEvent ffe " +
            "join fetch ffe.fightEvent " +
            "join fetch ffe.winner " +
            "join fetch ffe.loser " +
            "where ffe.fightEvent.completed = true " +
            "and ffe.canceled = false " +
            "and ffe.fightResult.draw = false " +
            "and ffe.fightResult.nc = false " +
            "and ffe.fightResult.winMethod in :winMethods")
     List<FighterFightEvent> findCompletedForHardGame(@Param("winMethods") List<WinMethod> winMethods);

    @Query("select ffe from FighterFightEvent ffe " +
            "join fetch ffe.fightEvent " +
            "join fetch ffe.winner " +
            "join fetch ffe.loser " +
            "where ffe.fightEvent.completed = true " +
            "and ffe.canceled = false " +
            "and ffe.fightResult.draw = false " +
            "and ffe.fightResult.nc = false " +
            "and ffe.fightResult.winMethod in :winMethods " +
            "and (ffe.winner.name in :names or ffe.loser.name in :names)")
    Set<FighterFightEvent> findCompletedForNormalGame(@Param("names") Set<String> names,
                                                      @Param("winMethods") List<WinMethod> winMethods);

    @Query("select ffe from FighterFightEvent ffe " +
            "join fetch ffe.winner " +
            "join fetch ffe.loser " +
            "where ffe.fightEvent.completed = true " +
            "and ffe.canceled = false " +
            "and ffe.fightResult.draw = false " +
            "and ffe.fightResult.nc = false " +
            "and ffe.winner.name = :name")
    List<FighterFightEvent> findCompletedByWinnerName(@Param("name") String name);

}
