package my.mma.api.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.CustomException;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.security.JWTUtil;
import my.mma.api.security.entity.Refresh;
import my.mma.api.security.repository.RefreshRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

// refresh 토큰의 사용처는 /reissue 하나이므로, CSRF 공격에 취약해져도 무방 (따라서 쿠키에 저장해도 됨)
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/reissue")
public class ReissueController {

    @Value("${spring.jwt.refresh.expiration}")
    private Long refreshExpireMs;

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    // refresh 토큰을 통해 access 토큰이 만료되었을 때 새로운 access 및 refresh 토큰 재발급
    @PostMapping("")
    public ResponseEntity<?> reissueRefresh(HttpServletRequest request) {
        String refresh = request.getHeader("Refresh");
        if (refresh == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("refresh token is null");
        }

        // [FIX Bug4] validateToken() 이 CustomException을 던질 수 있으나 try/catch 없이 호출하면
        // Spring의 기본 에러 핸들러가 500을 반환함 → 명시적으로 잡아 클라이언트에 적절한 응답 전달
        try {
            jwtUtil.validateToken(refresh);
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.extractCategory(refresh);
        if (!category.equals("refresh")) {
            return new ResponseEntity<>("invalid refresh token", HttpStatus.BAD_REQUEST);
        }
        boolean isExist = refreshRepository.existsById(refresh);
        if (!isExist) {
            return new ResponseEntity<>("refresh token is not existing in db", HttpStatus.BAD_REQUEST);
        }

        String newRefresh = jwtUtil.reissueToken(refresh, "refresh");
        String newAccess = jwtUtil.reissueToken(refresh, "access");

        // [FIX Bug6] 기존 코드: deleteById → save 순서였으나,
        // delete 성공 후 save 실패 시 refresh 토큰이 완전히 소실되어 사용자가 재로그인해야 함.
        // save를 먼저 수행해 새 토큰을 DB에 저장한 뒤 기존 토큰을 삭제하도록 순서 변경.
        // [FIX Bug5] existsById → deleteById 사이의 TOCTOU 레이스: 두 요청이 동시에 도달하면
        // 둘 다 existsById=true 를 받고 두 번 모두 새 토큰을 발급할 수 있음.
        // save-first 방식으로 구조 변경해 피해를 최소화하고, 운영 레벨에서는 분산 락 도입 권장.
        refreshRepository.save(Refresh.of(jwtUtil.extractEmail(newRefresh), newRefresh, refreshExpireMs));
        refreshRepository.deleteById(refresh);

        // response
        HashMap<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccess);
        tokens.put("refreshToken", newRefresh);
        return ResponseEntity.ok().body(tokens);
    }

}
