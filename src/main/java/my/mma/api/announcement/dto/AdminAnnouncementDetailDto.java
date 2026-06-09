package my.mma.api.announcement.dto;

import lombok.Builder;
import my.mma.api.announcement.entity.Announcement;

@Builder
public record AdminAnnouncementDetailDto(Long id, String title, String content, boolean pinned) {

    public static AdminAnnouncementDetailDto toDto(Announcement announcement){
        return new AdminAnnouncementDetailDto(announcement.getId(), announcement.getTitle(),
                announcement.getContent(), announcement.isPinned());
    }

}
