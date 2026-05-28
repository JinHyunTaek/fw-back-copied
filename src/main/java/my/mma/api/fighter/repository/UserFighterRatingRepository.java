package my.mma.api.fighter.repository;

import my.mma.api.fighter.entity.UserFighterRating;
import my.mma.api.game.dto.FighterNamePair;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserFighterRatingRepository extends JpaRepository<UserFighterRating, Long> {
    Optional<UserFighterRating> findByUserIdAndFighterId(Long userId, Long fighterId);
    @Query("select avg(r.rating) from UserFighterRating r where r.fighter.id=:fighterId")
    double findAvgRatingByFighterId(@Param("fighterId") Long fighterId);

    @Cacheable(value = "top100RatingFighters")
    @Query("select new my.mma.api.game.dto.FighterNamePair(f.name, f.koreanName) from UserFighterRating ufr" +
            " inner join ufr.fighter f" +
            " group by f.id, f.name, f.koreanName" +
            " order by avg(ufr.rating) desc")
    List<FighterNamePair> findTop100ByRating(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("delete from UserFighterRating r where r.user.id=:userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
