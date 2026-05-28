package my.mma.api.bet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import my.mma.api.user.entity.User;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fighter.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "idx_user_bets", columnList = "user_id,fight_event_id")
})
public class Bet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "bet_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 이미 지난 경기에 대해 해당 경기가 nc와 같은 예외 상황 발생하는 경우, 해당 배팅은 무효(null 처리)
    private Boolean succeed;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "fight_event_id")
    private FightEvent fightEvent;

    private int seedPoint;

    private boolean refunded;

    private Boolean settled;

    @Builder.Default
    @OneToMany(mappedBy = "bet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BetCard> betCards = new ArrayList<>();

    public void addBetCard(BetCard betCard) {
        betCard.addBet(this);
        this.betCards.add(betCard);
    }

    public void updateSucceed(boolean succeed) {
        this.succeed = succeed;
    }

    public void markSucceedNull() {
        this.succeed = null;
    }

    public void markRefunded() {
        this.refunded = true;
    }

    public void updateSettledToFalse() {
        this.settled = false;
    }

    public void updateSettledToTrue() {
        this.settled = true;
    }
}
