package my.mma.api.security.oauth2.service;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.util.Date;

@Slf4j
@Service
public class AppleTokenVerifyService {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${apple.bundle-id}")
    private String bundleId;

    public void verify(String identityToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(identityToken);

            JWKSet jwkSet = JWKSet.load(URI.create(APPLE_JWKS_URL).toURL());
            String keyId = signedJWT.getHeader().getKeyID();
            RSAKey rsaKey = (RSAKey) jwkSet.getKeyByKeyId(keyId);

            if (rsaKey == null) {
                throw new CustomException(ErrorCode.SOCIAL_TOKEN_VERIFY_FAILED_400);
            }

            RSASSAVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
            if (!signedJWT.verify(verifier)) {
                throw new CustomException(ErrorCode.SOCIAL_TOKEN_VERIFY_FAILED_400);
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (!APPLE_ISSUER.equals(claims.getIssuer())
                    || !claims.getAudience().contains(bundleId)
                    || new Date().after(claims.getExpirationTime())) {
                log.info("issuer={}",claims.getIssuer());
                log.info("audience={}",claims.getAudience());
                log.error("Apple token claims verify failed");
                throw new CustomException(ErrorCode.SOCIAL_TOKEN_VERIFY_FAILED_400);
            }
        } catch (Exception e) {
            log.error("Apple token verify failed", e);
            throw new CustomException(ErrorCode.SOCIAL_TOKEN_VERIFY_FAILED_400);
        }
    }
}
