package my.mma.api.event.promotion.entity;

import jakarta.persistence.*;
import lombok.*;
import my.mma.api.fighter.entity.BaseEntity;
import my.mma.api.user.entity.User;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_winner_promotion_user", columnNames = {"promotion_id","user_id"}))
public class PromotionWinner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_winner_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id")
    private User winner;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "gifticon_id",unique = true)
    private Gifticon gifticon;

    @Enumerated(EnumType.STRING)
    private SendStatus sendStatus;

    /** 추첨 당첨자 생성 (지급 대기 상태) */
    public static PromotionWinner of(Promotion promotion, User winner, Gifticon gifticon) {
        return PromotionWinner.builder()
                .promotion(promotion)
                .winner(winner)
                .gifticon(gifticon)
                .sendStatus(SendStatus.PENDING)
                .build();
    }

    public boolean isSent() {
        return sendStatus == SendStatus.SENT;
    }

    public void markSent() {
        this.sendStatus = SendStatus.SENT;
    }

    /** 기프티콘 발송 실패 (재시도 대상) */
    public void markFailed() {
        this.sendStatus = SendStatus.FAILED;
    }

}
