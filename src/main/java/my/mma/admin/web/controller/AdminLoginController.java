package my.mma.admin.web.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.dto.AdminLoginRequest;
import my.mma.admin.web.service.AdminLoginService;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.security.oauth2.dto.TokenResponse;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@Slf4j
@RequestMapping("/htj-admin")
@RequiredArgsConstructor
public class AdminLoginController {

    private final AdminLoginService adminLoginService;

    @GetMapping("/login")
    public String login(@ModelAttribute("user") AdminLoginRequest request) {
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("user") AdminLoginRequest request,
                        BindingResult bindingResult, HttpServletResponse response,
                        HttpServletRequest servletRequest) {
        if (bindingResult.hasErrors()) {
            return "admin/login";
        }
        TokenResponse tokenResponse = adminLoginService.login(request, servletRequest);
        if (tokenResponse == null) {
            bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
            return "admin/login";
        }
        Cookie access = new Cookie("accessToken", tokenResponse.accessToken());
        Cookie refresh = new Cookie("refreshToken", tokenResponse.refreshToken());
        adminLoginService.setCookieProperties(access, true);
        adminLoginService.setCookieProperties(refresh, false);
        response.addCookie(access);
        response.addCookie(refresh);
        return "redirect:/htj-admin/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response){
        adminLoginService.logout(request, response);
        return "redirect:/htj-admin/login";
    }

}
