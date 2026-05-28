package my.mma.api.announcement.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record AnnouncementDto(Long id, String title, boolean pinned, LocalDate createdDate){
}
