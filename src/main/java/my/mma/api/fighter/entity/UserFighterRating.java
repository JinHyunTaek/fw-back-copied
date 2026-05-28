package my.mma.api.fighter.entity;

import jakarta.persistence.*;
import lombok.*;
import my.mma.api.user.entity.User;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fighter_rating_user_fighter",
                columnNames = {"user_id", "fighter_id"}),
        indexes = @Index(name = "fighter_id_idx", columnList = "fighter_id")
)
public class UserFighterRating extends BaseEntity {

    @Id
    @Column(name = "fighter_rating_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rating;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "fighter_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Fighter fighter;

    public void updateRating(int newRating){
        this.rating = newRating;
    }

}
