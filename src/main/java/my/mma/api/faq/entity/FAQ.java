package my.mma.api.faq.entity;

import jakarta.persistence.*;
import lombok.*;
import my.mma.api.fighter.entity.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FAQ extends BaseEntity {

    @Id
    @Column(name = "faq_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    @Column(columnDefinition = "LONGTEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    private FAQCategory faqCategory;

    public void update(String question, String answer, FAQCategory category){
        this.question = question;
        this.answer = answer;
        this.faqCategory = category;
    }

}
