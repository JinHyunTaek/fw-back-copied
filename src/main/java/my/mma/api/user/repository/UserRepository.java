package my.mma.api.user.repository;

import jakarta.persistence.LockModeType;
import my.mma.api.user.dto.RankedUserDto;
import my.mma.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByNickname(String nickname);

    @Query("select new my.mma.api.user.dto.RankedUserDto(" +
            "u.id, u.nickname, u.earnedBetSucceedPoint, null)" +
            " from User u where u.earnedBetSucceedPoint > 0 order by u.earnedBetSucceedPoint desc")
    List<RankedUserDto> findTopRankedUsers(Pageable pageable);

    @Query("SELECT COUNT(u) + 1 FROM User u WHERE u.earnedBetSucceedPoint > :point")
    Integer findRankByPoint(@Param("point") int point);

    @Query("select u from User u where exists (" +
            "select 1 from Bet b where b.user=u and b.settled=false)")
    Page<User> findUsersWithUnsettledBets(Pageable pageable);

    Page<User> findUsersByNicknameContainsIgnoreCase(Pageable pageable, String nickname);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email=:email")
    User findByEmailWithLock(@Param("email") String email);

    @Modifying
    @Query("update User u set u.earnedBetSucceedPoint=0 where u.earnedBetSucceedPoint > 0")
    void initializeBetEarnedPoints();

    /**
     * FCM 이 영구 무효(UNREGISTERED 등)로 응답한 토큰을 비운다.
     * 벌크 연산은 영속성 컨텍스트를 우회하므로 1차 캐시가 낡지 않도록 clearAutomatically 를 켠다.
     */
    @Modifying(clearAutomatically = true)
    @Query("update User u set u.fcmToken = null where u.fcmToken in :tokens")
    int clearFcmTokens(@Param("tokens") Collection<String> tokens);

    User findByRoleEquals(String role);

}
