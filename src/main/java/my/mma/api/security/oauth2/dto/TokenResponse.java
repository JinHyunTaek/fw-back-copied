package my.mma.api.security.oauth2.dto;

public record TokenResponse(String accessToken, String refreshToken) {

    public static TokenResponse toDto(String accessToken, String refreshToken){
        return new TokenResponse(accessToken, refreshToken);
    }

}
