package my.mma.admin.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * prev_event 전용 DTO
 * 경기 결과 업데이트 용도로만 사용 (fighter 정보 불필요)
 */
public record CrawledPrevEvent(@JsonProperty("event_name") String eventName, @JsonProperty("cards") List<CrawledFightCard> crawledFightCards) {}
