package my.mma.api.fighter.repository;

import jakarta.persistence.LockModeType;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.game.dto.FighterNamePair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FighterRepository extends JpaRepository<Fighter, Long> {

    Page<Fighter> findByAvgRatingGreaterThan(double rating, Pageable pageable);

    List<Fighter> findAllByNameIn(List<String> names);
    Optional<Fighter> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fighter f where f.id=:id")
    Optional<Fighter> findByIdWithLock(Long id);

    @Query("select f from Fighter f where lower(f.name) like lower(concat('%', :keyword, '%'))" +
            " or (f.koreanName is not null and lower(f.koreanName) like lower(concat('%', :keyword, '%')))")
    Page<Fighter> searchByNameOrKoreanName(@Param("keyword") String keyword, Pageable pageable);

    List<Fighter> findFightersByRankingIsNotNull();

    Optional<Fighter> findByNameAndNicknameIsNotNull(String name);
    Optional<Fighter> findByNameAndFightRecordIsNotNull(String name);

    List<Fighter> findByNameInAndNationalityIsNotNull(@Param("names") List<String> names);

    @Query("select new my.mma.api.game.dto.FighterNamePair(f.name, f.koreanName) from Fighter f" +
            " where f.koreanName is not null")
    Set<FighterNamePair> findEveryNamePairs();

}
