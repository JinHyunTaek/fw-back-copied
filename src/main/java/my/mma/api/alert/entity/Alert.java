package my.mma.api.alert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import my.mma.api.alert.constant.AlertTarget;
import my.mma.api.fighter.entity.BaseEntity;
import my.mma.api.user.entity.User;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Table(name = "alerts", uniqueConstraints = {
        @UniqueConstraint(name = "user_target_unique", columnNames = {"user_id", "alert_target", "target_id"})
}, indexes = {
        @Index(name = "idx_alert_target", columnList = "alert_target,target_id")
})
@Entity
@AllArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PROTECTED)
@Getter
@Builder
public class Alert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "alert_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private AlertTarget alertTarget;

    private Long targetId;

}
