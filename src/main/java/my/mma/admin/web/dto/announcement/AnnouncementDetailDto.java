package my.mma.admin.web.dto.announcement;

import lombok.Builder;
import my.mma.api.announcement.entity.Announcement;

@Builder
public record AnnouncementDetailDto(Long id, String title, String content, boolean pinned) {

    public static AnnouncementDetailDto toDto(Announcement announcement){
        return new AnnouncementDetailDto(announcement.getId(), announcement.getTitle(),
                announcement.getContent(), announcement.isPinned());
    }

}
