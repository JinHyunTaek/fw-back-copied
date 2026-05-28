package my.mma.admin.web.dto.announcement;

import jakarta.validation.constraints.NotBlank;

// for update & save
public record AnnouncementRequest(@NotBlank String title, @NotBlank String content, boolean pinned) {
}
