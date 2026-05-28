package my.mma.api.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.security.filter.CustomLogoutFilter;
import my.mma.api.security.filter.LoginFilter;
import my.mma.api.security.repository.RefreshRepository;
import my.mma.api.security.service.FcmTokenUpdateService;
import my.mma.api.security.JWTUtil;
import my.mma.api.security.filter.JWTFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${spring.jwt.access.expiration}")
    private Long accessExpireMs;

    @Value("${spring.jwt.refresh.expiration}")
    private Long refreshExpireMs;

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final ObjectMapper objectMapper;
    private final FcmTokenUpdateService fcmTokenUpdateService;

    @Bean
    public BCryptPasswordEncoder encodePwd() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        try {
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            log.error("AuthenticationManager 초기화 실패", e);
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }
    }

    // actuator endpoint 전용
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.logout(logout -> logout.logoutUrl("/auth/logout"));

        http.authorizeHttpRequests(registry ->
                registry.requestMatchers("/user", "/reissue", "/auth/social_login",
                                "/smtp/**", "/user/dup_nickname", "/user/password-reset",
                                "/ws/**", "/css/**","/htj-admin/login","/app-status"
                                // [FIX Bug24] /actuator/** 를 permitAll에서 제거.
                                // actuator 엔드포인트는 내부망 또는 별도 인증으로 보호해야 함.
                                // 필요 시 application.yml의 management.endpoints.web.exposure.include 로 노출 범위 제한.
                        ).permitAll()
                        .requestMatchers("/htj-admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(new JWTFilter(jwtUtil, refreshRepository, refreshExpireMs), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAt(new LoginFilter(authenticationManager(), jwtUtil, refreshRepository, accessExpireMs, refreshExpireMs, objectMapper, fcmTokenUpdateService),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository), LogoutFilter.class);
        return http.build();
    }
}
