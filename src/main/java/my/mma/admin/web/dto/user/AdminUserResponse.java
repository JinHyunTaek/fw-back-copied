package my.mma.admin.web.dto.user;

import my.mma.api.user.entity.User;

public record AdminUserResponse(long id, String nickname, String email, String profileImgUrl) {

    public static AdminUserResponse toDto(User user, String profileImgUrl){
        return new AdminUserResponse(user.getId(), user.getNickname(), user.getEmail(), profileImgUrl);
    }

}
