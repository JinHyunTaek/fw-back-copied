package my.mma.api.report.entity;

import lombok.*;
import my.mma.api.report.dto.ReportRequest;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Duration;
import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@RedisHash(value = "reported_message")
public class ReportedMessageData {

    private static final int expirationDays = 10;

    @Id
    private String messageId;

    private String messageSnapshot;

    @TimeToLive
    private long expiration;

    @Indexed
    private Long reportedUserId;

    private int reportedCount;

    private LocalDate firstReportedDate;

    public static ReportedMessageData of(ReportRequest request) {
        return ReportedMessageData.builder()
                .messageId(request.messageId())
                .messageSnapshot(request.messageSnapshot())
                .expiration(Duration.ofDays(expirationDays).toSeconds())
                .reportedUserId(request.reportedUserId())
                .firstReportedDate(LocalDate.now())
                .reportedCount(1)
                .build();
    }

    public void increaseReportedCount() {
        this.reportedCount++;
    }

}
