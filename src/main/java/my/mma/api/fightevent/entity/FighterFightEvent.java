package my.mma.api.fightevent.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import my.mma.api.fightevent.entity.property.FightResult;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fighter.entity.BaseEntity;
import my.mma.api.fighter.entity.Fighter;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FighterFightEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fighter_fight_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Fighter winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loser_id")
    private Fighter loser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fight_event_id")
    private FightEvent fightEvent;

    private FightWeight fightWeight;

    @Embedded
    private FightResult fightResult;

    private boolean title;

    @Column(name = "is_canceled")
    private boolean canceled;

    private int cardOrder;

    // Fight Of The night
    @Column(name = "is_fotn")
    private boolean fotN;

    // Performance of the night
    @Column(name = "is_potn")
    private boolean potN;

    protected void addFightEvent(FightEvent fightEvent) {
        this.fightEvent = fightEvent;
    }

    public void updateFromCrawledPrevFight(FightResult fightResult,
                                           boolean draw,
                                           boolean nc,
                                           boolean isFotN,
                                           boolean isPotN) {
        this.fightResult = fightResult;
        this.fightResult.setDraw(draw);
        this.fightResult.setNc(nc);
        this.fotN = isFotN;
        this.potN = isPotN;
    }

    public void updateBonuses(boolean isFotN, boolean isPotN) {
        this.fotN = isFotN;
        this.potN = isPotN;
    }

    public void swapWinnerAndLoser() {
        Fighter temp = winner;
        winner = loser;
        loser = temp;
    }

    public void updateStatusToCanceled() {
        this.canceled = true;
    }

    public void reactivate(int cardOrder) {
        this.canceled = false;
        this.cardOrder = cardOrder;
    }

    public void updateCardOrder(int order) {
        this.cardOrder = order;
    }

    public void updateDetails(FightWeight fightWeight, boolean title) {
        this.fightWeight = fightWeight;
        this.title = title;
    }

}