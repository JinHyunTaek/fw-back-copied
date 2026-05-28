package my.mma.api.bet.entity;

import jakarta.persistence.*;
import lombok.*;
import my.mma.api.fighter.entity.BaseEntity;
import my.mma.api.fightevent.entity.FighterFightEvent;

@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(
        indexes = @Index(name = "ffe_id", columnList = "fighter_fight_event_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"fighter_fight_event_id"})
)
@Getter
public final class FightPickCount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bet_rate_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fighter_fight_event_id")
    private FighterFightEvent fighterFightEvent;

    private int firstFighterPickCount;

    private int lastFighterPickCount;

}
