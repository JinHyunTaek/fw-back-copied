package my.mma.fixture.dto.auth;

import my.mma.api.security.CustomUserDetails;
import my.mma.api.security.oauth2.dto.TempUserDto;

public class CustomUserDetailsFixture {

    public static final String AUTH_EMAIL = "email123@google.com";

    public static CustomUserDetails createCustomUserDetails() {
        TempUserDto tempUser = TempUserDto.builder()
                .email(AUTH_EMAIL)
                .nickname("nickname123")
                .password("pwd123")
                .role("ROLE_USER")
                .build();
        return new CustomUserDetails(tempUser);
    }

}
