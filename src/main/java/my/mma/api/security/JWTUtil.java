package my.mma.api.security;

import io.jsonwebtoken.*;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.security.dto.JwtCrateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Component
public class JWTUtil {

    @Value("${spring.jwt.refresh.expiration}")
    private Long refreshExpireMs;

    @Value("${spring.jwt.access.expiration}")
    private Long accessExpireMs;

    private final SecretKey secretKey;

    public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractCategory(String token) {
        return extractClaim(token, claims -> claims.get("category", String.class));
    }

    public boolean extractIsSocial(String token) {
        return extractClaim(token, claims -> claims.get("isSocial", Boolean.class));
    }

    public String extractDomain(String token) {
        return extractClaim(token, claims -> claims.get("domain", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String createJwt(JwtCrateDto crateDto) {
        return crateDto.toJwtToken(secretKey);
    }

    public String reissueToken(String prevToken, String category) {
        return createJwt(JwtCrateDto.toDto(
                category,
                extractEmail(prevToken),
                extractRole(prevToken),
                category.equals("access") ? accessExpireMs : refreshExpireMs,
                extractDomain(prevToken),
                extractIsSocial(prevToken)
        ));
    }

    public void validateToken(String token) {
        try {
            // [FIX Bug17/18] 기존 코드는 .build().parseSignedClaims() 를 호출하지 않아
            // 실제 서명/만료 검증이 전혀 되지 않았음.
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.JWT_TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.UNSUPPORTED_JWT);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // [Bug19] 넓은 Exception catch 는 의도적 — isValid 는 boolean 반환용이므로 유지
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }
}
