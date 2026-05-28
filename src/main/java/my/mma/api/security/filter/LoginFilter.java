package my.mma.api.security.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.security.repository.RefreshRepository;
import my.mma.api.security.service.FcmTokenUpdateService;
import my.mma.api.security.JWTUtil;
import my.mma.api.security.dto.JwtCrateDto;
import my.mma.api.security.entity.Refresh;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final Long accessExpireMs;
    private final Long refreshExpireMs;
    private final ObjectMapper objectMapper;
    private final FcmTokenUpdateService fcmTokenUpdateService;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil,
                       RefreshRepository refreshRepository,
                       Long accessExpireMs, Long refreshExpireMs,
                       ObjectMapper objectMapper, FcmTokenUpdateService fcmTokenUpdateService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
        this.accessExpireMs = accessExpireMs;
        this.refreshExpireMs = refreshExpireMs;
        this.objectMapper = objectMapper;
        this.fcmTokenUpdateService = fcmTokenUpdateService;
        setFilterProcessesUrl("/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        String email;
        String password;
        String fcmToken;
        try {
            String requestBody = getRequestBody(request);
            Map<String, String> jsonMap = objectMapper.readValue(requestBody, new TypeReference<>() {
            });
            email = jsonMap.get("email");
            password = jsonMap.get("password");
            fcmToken = jsonMap.get("fcmToken");
        } catch (IOException e) {
            log.error("[LOGIN] error = ", e);
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, null);
        authToken.setDetails(fcmToken);
        return authenticationManager.authenticate(authToken);
    }

    @Override
    public void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                         FilterChain chain, Authentication authentication) throws IOException {
        String email = authentication.getName();

        // [FIX Bug14] authentication.getDetails() 는 Object 타입.
        // Spring Security 내부 처리 중 details 가 WebAuthenticationDetails 등으로 교체될 수 있어
        // 무조건 (String) 캐스팅 시 ClassCastException 발생 가능 → instanceof 로 안전하게 처리.
        Object detailsObj = authentication.getDetails();
        String fcmToken = (detailsObj instanceof String s) ? s : null;

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();

        // [FIX Bug12] authorities 가 비어있을 경우 iterator.next() 에서 NoSuchElementException 발생.
        // 정상적인 사용자라면 발생하지 않지만 방어 로직 추가.
        if (!iterator.hasNext()) {
            log.error("No authorities found for user: {}", email);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        String access = jwtUtil.createJwt(JwtCrateDto.toDto(
                "access", email, role, accessExpireMs, "LOCAL", false
        ));
        String refresh = jwtUtil.createJwt(JwtCrateDto.toDto(
                "refresh", email, role, refreshExpireMs, "LOCAL", false
        ));

        if (fcmToken != null) {
            fcmTokenUpdateService.updateUserFcmToken(email, fcmToken);
        }

        addRefreshEntity(email, refresh, refreshExpireMs);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HashMap<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", access);
        tokens.put("refreshToken", refresh);
        String responseBody = objectMapper.writeValueAsString(tokens);
        response.getWriter().write(responseBody);
        response.setStatus(HttpStatus.OK.value());
    }

    private void addRefreshEntity(String username, String refresh, Long expiredMs) {
        refreshRepository.save(Refresh.of(username, refresh, expiredMs));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) {
        log.warn("[LOGIN] fail ip={} reason={}", request.getRemoteAddr(), failed.getMessage());
        response.setStatus(401);
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
