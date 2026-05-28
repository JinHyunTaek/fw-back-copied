package my.mma.api.alert.repository;

import my.mma.api.alert.entity.Alert;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    boolean existsByUserAndAlertTargetAndTargetId(User user, AlertTarget targetType, Long targetId);

    void deleteByUserAndAlertTargetAndTargetId(User user, AlertTarget targetType, Long targetId);

    List<Alert> findByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Alert a where a.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
            delete from Alert a
            where a.user.id = :userId
            and a.alertTarget = :alertTarget
            """)
    void deleteAllByUserIdAndAlertTarget(@Param("userId") Long userId, @Param("alertTarget") AlertTarget alertTarget);

    @EntityGraph(attributePaths = {"user"})
    List<Alert> findByAlertTargetAndTargetIdIn(AlertTarget alertTarget, Set<Long> targetIds);

    @EntityGraph(attributePaths = {"user"})
    List<Alert> findByAlertTargetAndTargetId(AlertTarget alertTarget, Long targetId);

    @Query("""
            select u.fcmToken
            from Alert a
            join a.user u
            where a.alertTarget = :alertTarget
            and a.targetId = :targetId
            and u.fcmToken is not null
            """)
    List<String> findUserTokens(@Param("alertTarget") AlertTarget alertTarget, @Param("targetId") Long targetId);
}
