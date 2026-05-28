package my.mma.api.security.oauth2.handler;

import lombok.extern.slf4j.Slf4j;
import my.mma.api.security.JWTUtil;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Slf4j
//@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    public CustomSuccessHandler(JWTUtil jwtUtil) {

        this.jwtUtil = jwtUtil;
    }

//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//        //OAuth2User
//        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
//
//        String email = customUserDetails.getEmail();
//
//        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
//        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
//        GrantedAuthority auth = iterator.next();
//        String role = auth.getAuthority();
//
//        String access = jwtUtil.createJwt("access", email, role, 600000L);
//        String refresh = jwtUtil.createJwt("refresh",email,role,86400000L);
//
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//
//        ObjectMapper objMapper = new ObjectMapper();
//        HashMap<String, String> tokens = new HashMap<>();
//        tokens.put("accessToken",access);
//        tokens.put("refreshToken",refresh);
//        String responseBody = objMapper.writeValueAsString(tokens);
//        response.getWriter().write(responseBody);
//        response.setStatus(HttpStatus.OK.value());
//        log.info("custom success handler executed");
////        response.sendRedirect("http://localhost:8080?accessToken="+access+"&refreshToken="+refresh);
//    }

}