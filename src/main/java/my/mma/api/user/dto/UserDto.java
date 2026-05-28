package my.mma.api.user.dto;

import lombok.*;
import my.mma.api.report.entity.ReportCategory;
import my.mma.api.user.entity.User;

import java.time.LocalDateTime;

@Builder
public record UserDto(long id, String nickname, String email, String role, int point, int earnedBetSucceedPoint,
                      ReportCategory reportedReason, LocalDateTime restrictEndAt, String profileImgUrl) {

    public static UserDto toDto(User user, String profileImgUrl, ReportCategory reason, LocalDateTime restrictEndAt){
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .point(user.getPoint())
                .earnedBetSucceedPoint(user.getEarnedBetSucceedPoint())
                .reportedReason(reason)
                .profileImgUrl(profileImgUrl)
                .restrictEndAt(restrictEndAt)
                .build();
    }

}
