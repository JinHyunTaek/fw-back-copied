package my.mma.api.event.promotion.entity;

import jakarta.persistence.*;
import lombok.*;
import my.mma.api.fighter.entity.BaseEntity;

import java.time.LocalDate;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Gifticon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gifticon_id")
    private Long id;

    private String name;

    @Column(unique = true)
    private String couponNumber;

    private LocalDate expiryDate; // 유효기간

    private String imageKey; // s3 key

    private boolean isAssigned;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    private GifticonCategory category;

    private int displayOrder; // 낮을수록 상위(1등). rank/priority와 동일 개념

    public void update(String name, String couponNumber, LocalDate expiryDate,
                       GifticonCategory category, int displayOrder) {
        this.name = name;
        this.couponNumber = couponNumber;
        this.expiryDate = expiryDate;
        this.category = category;
        this.displayOrder = displayOrder;
    }

    public void changeImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    /** 추첨으로 당첨자에게 배정 */
    public void assign() {
        this.isAssigned = true;
    }
}
