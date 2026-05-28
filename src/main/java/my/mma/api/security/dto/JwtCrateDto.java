package my.mma.api.security.dto;

import io.jsonwebtoken.Jwts;
import lombok.*;

import javax.crypto.SecretKey;
import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtCrateDto {

    private String category;
    private String email;
    private String role;
    private String domain;
    private boolean social;
    private Long expireMs;

    public static JwtCrateDto toDto(String category, String email, String role, Long expireMs, String domain, boolean isSocial) {
        return JwtCrateDto.builder()
                .category(category)
                .email(email)
                .role(role)
                .domain(domain)
                .expireMs(expireMs)
                .social(isSocial)
                .build();
    }

    public String toJwtToken(SecretKey secretKey) {
        return Jwts.builder()
                .claim("category", category)
                .claim("email", email)
                .claim("role", role)
                .claim("isSocial", social)
                .claim("domain", domain)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(secretKey)
                .compact();
    }

}
