package my.mma.api.announcement.dto;

import jakarta.validation.constraints.NotBlank;

// for update & save
public record AdminAnnouncementRequest(@NotBlank String title, @NotBlank String content, boolean pinned) {
}
