package my.mma.admin.web.login.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.login.dto.AdminLoginRequest;
import my.mma.admin.web.user.repository.AdminUserRepository;
import my.mma.api.security.JWTUtil;
import my.mma.api.security.dto.JwtCrateDto;
import my.mma.api.security.entity.Refresh;
import my.mma.api.security.oauth2.dto.TokenResponse;
import my.mma.api.security.repository.RefreshRepository;
import my.mma.api.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminLoginService {

    @Value("${spring.jwt.refresh.expiration}")
    private Long refreshExpireMs;

    @Value("${spring.jwt.access.expiration}")
    private Long accessExpireMs;

    private final AdminUserRepository adminUserRepository;
    private final RefreshRepository refreshRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    public TokenResponse login(AdminLoginRequest request, HttpServletRequest servletRequest) {
        log.info("[ADMIN LOGIN] email={}, ip={}", request.email(), servletRequest.getRemoteAddr());
        User user = adminUserRepository.findByRoleEqualsAndEmail("ROLE_ADMIN", request.email())
                .orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return null;
        }
        String access = jwtUtil.createJwt(JwtCrateDto.toDto(
                "access", request.email(), "ROLE_ADMIN", accessExpireMs, null, false
        ));
        String refresh = jwtUtil.createJwt(JwtCrateDto.toDto(
                "refresh", request.email(), "ROLE_ADMIN", refreshExpireMs, null, false
        ));
        addRefreshEntity(request.email(), refresh, refreshExpireMs);
        return new TokenResponse(access, refresh);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refreshToken")) {
                refreshRepository.deleteById(cookie.getValue());
            }
            if ("accessToken".equals(cookie.getName()) ||
                    "refreshToken".equals(cookie.getName())) {

                Cookie deleteCookie = new Cookie(cookie.getName(), null);
                deleteCookie.setMaxAge(0);
                deleteCookie.setPath("/");
                deleteCookie.setHttpOnly(true);
                response.addCookie(deleteCookie);
            }
        }
        SecurityContextHolder.clearContext();
    }

    public void setCookieProperties(Cookie cookie, boolean isAccess) {
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge((int) (isAccess ? accessExpireMs / 1000 : refreshExpireMs / 1000));
    }

    private void addRefreshEntity(String email, String refresh, Long expiredMs) {
        refreshRepository.save(Refresh.builder()
                .email(email)
                .token(refresh)
                .expiration(Duration.ofMillis(expiredMs).toSeconds())
                .build());
    }

}
