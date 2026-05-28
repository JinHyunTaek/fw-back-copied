package my.mma.api.alert.repository;

import my.mma.api.alert.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {
    UserPreferences findByUserId(Long userId);
    @Query("select distinct u.fcmToken from UserPreferences up join up.user u where up.weeklyEventAlertEnabled = true and u.fcmToken is not null")
    List<String> findFcmTokensByWeeklyEventAlertEnabledIsTrue();

    @Modifying(clearAutomatically = true)
    @Query("delete from UserPreferences up where up.user.id=:userId")
    void deleteUserPreferencesByUserId(@Param("userId") Long userId);
}
