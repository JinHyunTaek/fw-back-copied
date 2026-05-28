package my.mma.api.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.fighter.entity.BaseEntity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Builder
@AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", indexes = {@Index(name = "idx_earned_bet_succeed_point", columnList = "earned_bet_succeed_point")})
public class User extends BaseEntity {

    public static int JOIN_POINT = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true)
    private String email;

    // 'google'/'naver'/'kakao' + 해당 도메인으로부터 발급하는 아이디(번호)
    private String username;

    @Column(unique = true)
    private String nickname;

    private String password;

    private String role;

    @Min(value = 0)
    private int point;

    private int earnedBetSucceedPoint;

    private String fcmToken;

    private LocalDateTime nicknameLastModifiedAt;

    public void updateNickname(String nickname) {
        if (this.nickname == null) {
            this.nickname = nickname;
        }else {
            if (nicknameLastModifiedAt != null && ChronoUnit.DAYS.between(nicknameLastModifiedAt, LocalDateTime.now()) < 7)
                throw new CustomException(ErrorCode.UPDATE_NICKNAME_NOT_AVAILABLE_403);
            this.nickname = nickname;
            this.nicknameLastModifiedAt = LocalDateTime.now();
        }
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updatePoint(int point) {
        this.point = point;
    }

    public void updateEarnedBetSucceedPoint(int earnedBetSucceedPoint) {
        this.earnedBetSucceedPoint = earnedBetSucceedPoint;
    }

    public void accumulateEarnedBetSucceedPoint(int newAccumulatedPoint) {
        this.earnedBetSucceedPoint += newAccumulatedPoint;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

}