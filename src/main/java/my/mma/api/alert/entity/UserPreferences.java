package my.mma.api.alert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import my.mma.api.user.entity.User;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@AllArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PROTECTED)
@Getter
@Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name="user_preferences_id")
    private Long id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private boolean fighterAlertEnabled;
    private boolean upcomingEventAlertEnabled;
    private boolean weeklyEventAlertEnabled;

    public void toggleFighterAlertEnabled(){
        fighterAlertEnabled = !fighterAlertEnabled;
    }

    public void toggleUpcomingEventAlertEnabled(){
        upcomingEventAlertEnabled = !upcomingEventAlertEnabled;
    }

    public void toggleWeeklyEventAlertEnabled(){
        weeklyEventAlertEnabled = !weeklyEventAlertEnabled;
    }

    public void toggleAll(boolean isOn){
        this.fighterAlertEnabled = isOn;
        this.upcomingEventAlertEnabled = isOn;
        this.weeklyEventAlertEnabled = isOn;
    }

    public static UserPreferences of(User user){
        return UserPreferences.builder()
                .user(user)
                .fighterAlertEnabled(true)
                .weeklyEventAlertEnabled(true)
                .upcomingEventAlertEnabled(true)
                .build();
    }

}
