package my.mma.api.user.dto;

import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(@Size(min = 2, max = 10) String nickname) {
}
