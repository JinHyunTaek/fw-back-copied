package my.mma.api.report.entity;


import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Duration;

@RedisHash(value = "report_dedup")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class ReportDedup {

    @Id
    private String id; // reporterId:messageId

    @Indexed
    private String messageId;

    @TimeToLive
    private Long expiration;

    private ReportCategory reportCategory;

    public static ReportDedup of(Long reporterId, String messageId, ReportCategory reportCategory) {
        return ReportDedup.builder()
                .id(reporterId + ":" + messageId)
                .messageId(messageId)
                .expiration(Duration.ofDays(10).toSeconds())
                .reportCategory(reportCategory)
                .build();
    }

}
