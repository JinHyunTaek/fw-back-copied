package my.mma.api.fighter.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        indexes = @Index(name = "avg_rating_idx", columnList = "avg_rating")
)
public class Fighter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fighter_id")
    private Long id;

    @Column(unique = true)
    private String name;

    @Column(unique = true)
    private String koreanName;

    private String nickname;

    private Integer height;

    private Double weight;

    @Embedded
    private FightRecord fightRecord;

    private Integer ranking;

    private LocalDate birthday;

    private Integer reach;

    private double avgRating;

    @Enumerated(EnumType.STRING)
    private Country nationality;

    public void updateFightRecord(String[] newFightRecord) {
        this.fightRecord = FightRecord.toFightRecord(newFightRecord);
    }

    public void updateRanking(Integer ranking) {
        this.ranking = ranking;
    }

    public void updateAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public void updateFighterForAdmin(String name, String koreanName, String nickname, FightRecord record,
                                      Integer reach, Double weight, Integer height, LocalDate birthday, Country nationality) {
        this.name = name;
        this.koreanName = koreanName;
        this.nickname = nickname;
        this.fightRecord = record;
        this.reach = reach;
        this.weight = weight;
        this.height = height;
        this.birthday = birthday;
        this.nationality = nationality;
    }

}