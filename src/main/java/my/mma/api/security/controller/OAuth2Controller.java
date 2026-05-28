package my.mma.api.security.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.security.oauth2.dto.TokenResponse;
import my.mma.api.security.oauth2.dto.TokenVerifyRequest;
import my.mma.api.security.oauth2.service.AppleTokenVerifyService;
import my.mma.api.security.oauth2.service.OAuth2Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class OAuth2Controller {

    private final RestTemplate restTemplate;
    private final OAuth2Service oAuth2Service;
    private final AppleTokenVerifyService appleTokenVerifyService;

    @PostMapping("/social_login")
    public ResponseEntity<TokenResponse> socialLogin(
            @Validated @RequestBody TokenVerifyRequest request
    ) {
        if(request.domain().equals("APPLE")){
            appleTokenVerifyService.verify(request.accessToken());
            return ResponseEntity.ok().body(oAuth2Service.saveUserIfNotExists(request));
        }

        String verifyUrl = switch (request.domain()) {
            case "KAKAO" -> "https://kapi.kakao.com/v1/user/access_token_info";
            case "NAVER" -> "https://openapi.naver.com/v1/nid/verify";
            case "GOOGLE" -> "https://www.googleapis.com/oauth2/v1/userinfo";
            default -> null;
        };

        if (verifyUrl == null)
            throw new CustomException(ErrorCode.BAD_REQUEST_400);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + request.accessToken());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    verifyUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
        } catch (HttpStatusCodeException e) {
            throw new CustomException(ErrorCode.SOCIAL_TOKEN_VERIFY_FAILED_400);
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok().body(oAuth2Service.saveUserIfNotExists(request));
        }
        throw new CustomException(ErrorCode.SOCIAL_TOKEN_VERIFY_FAILED_400);
    }
}
