package my.mma.api.user.entity;

import lombok.Builder;
import my.mma.api.report.entity.ReportCategory;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash(value = "punished_user")
@Builder
public record PunishedUser(@Id Long userId, ReportCategory reportCategory, @TimeToLive long expiration) {
}
