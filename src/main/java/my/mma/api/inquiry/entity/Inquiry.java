package my.mma.api.inquiry.entity;

import jakarta.persistence.*;
import lombok.*;
import my.mma.api.inquiry.entity.constant.InquiryCategory;
import my.mma.api.fighter.entity.BaseEntity;
import my.mma.api.user.entity.User;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private InquiryCategory category;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(columnDefinition = "LONGTEXT")
    private String answer;

    public void updateAnswer(String answer) {
        this.answer = answer;
    }
}
