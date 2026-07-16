package my.mma.admin.web.user.dto;

import lombok.Builder;
import my.mma.api.user.entity.User;

@Builder
public record AdminUserDetailResponse(Long id, String nickname, String email,
                                      int point, int earnedBetSucceedPoint, boolean punished, String profileImgUrl) {

    public static AdminUserDetailResponse toDto(User user, boolean punished, String profileImgUrl){
        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .point(user.getPoint())
                .earnedBetSucceedPoint(user.getEarnedBetSucceedPoint())
                .punished(punished)
                .profileImgUrl(profileImgUrl)
                .build();
    }

}
