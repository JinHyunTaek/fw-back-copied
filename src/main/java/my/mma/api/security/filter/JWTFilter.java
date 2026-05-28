package my.mma.api.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.CustomException;
import my.mma.api.security.CustomUserDetails;
import my.mma.api.security.JWTUtil;
import my.mma.api.security.entity.Refresh;
import my.mma.api.security.oauth2.dto.TempUserDto;
import my.mma.api.security.repository.RefreshRepository;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final Long refreshExpireMs;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final UUID uuid = UUID.randomUUID();
        MDC.put("requestId", uuid.toString());
        try {
            if (request.getRequestURI().startsWith("/htj-admin")) {
                if (!handleAdminAuth(request, response)) {
                    return; // access, refresh 모두 만료(login page로 redirect)
                }
            } else {
                String authorization = request.getHeader("Authorization");
                // 토큰이 없다면 다음 필터로 넘김
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                // [FIX Bug11] split(" ") 결과가 2개 미만일 수 있음 — 배열 범위 초과 방지
                String[] parts = authorization.split(" ");
                if (parts.length < 2) {
                    handleException(response, "Invalid Authorization header format", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                String accessToken = parts[1];

                try {
                    jwtUtil.validateToken(accessToken);
                } catch (CustomException e) {
                    log.error("e=",e);
                    handleException(response, "Invalid jwt token", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                if (!jwtUtil.extractCategory(accessToken).equals("access")) {
                    handleException(response, "Invalid token category", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                setAuthentication(accessToken);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/css")
                || uri.startsWith("/js")
                || uri.startsWith("/images")
                || uri.equals("/favicon.ico")
                || uri.equals("/htj-admin/login")
                || uri.startsWith("/app-status");
    }

    private void setAuthentication(String token) {
        TempUserDto userDto = TempUserDto.builder()
                .role(jwtUtil.extractRole(token))
                .email(jwtUtil.extractEmail(token))
                .password("social_login_no_password")
                .build();
        CustomUserDetails customUserDetails = new CustomUserDetails(userDto);
        // 일시적으로 생성되는 세션 (사용자 등록)
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void handleException(HttpServletResponse response, String message, int statusCode) throws IOException {
        log.warn(message);
        response.setStatus(statusCode);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    private String extractFromCookie(HttpServletRequest request, String name) {
        // [FIX Bug9] System.out.println(Arrays.toString(request.getCookies())) 제거 — 쿠키 전체가 로그에 노출되는 보안 취약점
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean handleAdminAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessToken = extractFromCookie(request, "accessToken");
        if (accessToken != null && jwtUtil.isValid(accessToken) && jwtUtil.extractCategory(accessToken).equals("access")) {
            // 만약 accessToken role admin x -> login page redirection
            if (!jwtUtil.extractRole(accessToken).equals("ROLE_ADMIN")) {
                response.sendRedirect("/htj-admin/login");
                return false;
            }
            setAuthentication(accessToken);
            return true;
        }
        // access expired -> check refresh
        String refreshToken = extractFromCookie(request, "refreshToken");
        // [FIX Bug8] refreshToken의 category가 "refresh"인지 검증하지 않으면
        // access 토큰을 refreshToken 쿠키에 담아 전송하는 공격에 취약해짐
        if (refreshToken == null || !jwtUtil.isValid(refreshToken)
                || !jwtUtil.extractCategory(refreshToken).equals("refresh")) {
            log.warn("[ADMIN] Redirect to login page: refreshToken is null, invalid, or wrong category");
            response.sendRedirect("/htj-admin/login");
            return false;
        }
        // [FIX Bug8] refresh 토큰의 role도 ADMIN인지 확인
        if (!jwtUtil.extractRole(refreshToken).equals("ROLE_ADMIN")) {
            log.warn("[ADMIN] Redirect to login page: refreshToken role is not ADMIN");
            response.sendRedirect("/htj-admin/login");
            return false;
        }
        // DB에 존재하는 refresh 토큰인지 확인 — 탈취·폐기된 토큰으로 무한 재발급 방지
        if (!refreshRepository.existsById(refreshToken)) {
            log.warn("[ADMIN] Redirect to login page: refreshToken not found in DB");
            response.sendRedirect("/htj-admin/login");
            return false;
        }

        // refresh 유효 → 새 access/refresh 재발급
        String newRefreshToken = jwtUtil.reissueToken(refreshToken, "refresh");
        String newAccessToken = jwtUtil.reissueToken(refreshToken, "access");
        refreshRepository.save(Refresh.of(jwtUtil.extractEmail(newRefreshToken), newRefreshToken, refreshExpireMs));
        refreshRepository.deleteById(refreshToken);

        Cookie accessCookie = new Cookie("accessToken", newAccessToken);
        Cookie refreshCookie = new Cookie("refreshToken", newRefreshToken);
        setCookieProperties(response, accessCookie);
        setCookieProperties(response, refreshCookie);
        // [FIX Bug7] 기존 setAuthentication(refreshToken) — refreshToken으로 인증 컨텍스트를 설정하면
        // extractEmail/Role이 refresh 토큰 기준으로 동작. newAccessToken으로 교체해야 정확함.
        setAuthentication(newAccessToken);
        return true;
    }

    private static void setCookieProperties(HttpServletResponse response, Cookie cookie) {
        cookie.setHttpOnly(true);
        // [FIX Bug10] Secure 플래그 누락 — HTTPS가 아닌 연결에서 쿠키가 전송될 수 있음
        cookie.setSecure(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

}
