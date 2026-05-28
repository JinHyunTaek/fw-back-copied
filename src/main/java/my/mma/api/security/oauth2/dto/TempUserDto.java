package my.mma.api.security.oauth2.dto;

import lombok.*;

@Getter
@Setter
@Builder
public class TempUserDto {

    private String role;
    private String nickname;
    private String email;
    private String password;
}