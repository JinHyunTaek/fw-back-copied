package my.mma.api.event.promotion.entity;

import jakarta.persistence.*;
import lombok.*;
import my.mma.api.fighter.entity.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Promotion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long id;

    private String title;

    private String benefit;

    private LocalDate startDate; // 이벤트 시작 날짜

    private LocalDate endDate; // 이벤트 종료 날짜

    private LocalDate announceDate; // 앱에서 실제로 조회 가능한 날짜

    private int maxWinnerCount;

    @Column(length = 1000)
    private String notice; // 유의사항

    private LocalDateTime drawnAt; // 추첨 실행 시각 (null = 미추첨). 이후 수정/삭제/추가 잠금 기준

    /** 추첨 완료 여부 — 이 값이 true면 프로모션/기프티콘 구성 변경 불가 */
    public boolean isDrawn() {
        return drawnAt != null;
    }

    /** 추첨 실행 시 호출 (당첨자 배정과 같은 트랜잭션에서) */
    public void markDrawn() {
        this.drawnAt = LocalDateTime.now();
    }

    public void update(String title, String benefit, LocalDate startDate, LocalDate endDate,
                       LocalDate announceDate, int maxWinnerCount, String notice){
        this.title = title;
        this.benefit = benefit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.announceDate = announceDate;
        this.maxWinnerCount = maxWinnerCount;
        this.notice = notice;
    }

    public void updateMaxWinnerCount(int newMaxWinnerCount){
        this.maxWinnerCount = newMaxWinnerCount;
    }

}
