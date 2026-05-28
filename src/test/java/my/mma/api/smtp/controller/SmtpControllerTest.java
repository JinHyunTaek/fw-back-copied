package my.mma.api.smtp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.mma.api.config.TestSecurityConfig;
import my.mma.api.smtp.dto.EmailVerificationCodeRequest;
import my.mma.api.smtp.dto.EmailVerificationSendResult;
import my.mma.api.smtp.dto.VerifyCodeRequest;
import my.mma.api.smtp.service.SmtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static my.mma.api.exception.ErrorCode.VALIDATION_FAILED_400;
import static my.mma.api.smtp.dto.EmailVerificationSendResult.EMAIL_ALREADY_EXISTS;
import static my.mma.api.smtp.dto.EmailVerificationSendResult.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SmtpController.class})
@Import(TestSecurityConfig.class)
class SmtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SmtpService smtpService;

    @DisplayName("인증 코드 전송 완료 응답(true)")
    @Test
    void sendJoinCode_whenSendJoinCodeIsTrue() throws Exception {
        //given
        String email = "email123@google.com";
        EmailVerificationCodeRequest request = new EmailVerificationCodeRequest(email, true);
        when(smtpService.sendEmailVerificationCode(request)).thenReturn(SUCCESS);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post("/smtp/verification-code-transmission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
        EmailVerificationSendResult res = objectMapper.readValue(responseBodyAsString,EmailVerificationSendResult.class);

        //then
        assertThat(res).isEqualTo(SUCCESS);
    }

    @DisplayName("인증 코드 전송하지 않은 경우(이미 가입된 이메일 계정) 응답(false)")
    @Test
    void dontSendJoinCode_whenSendJoinCodeIsFalse() throws Exception {
        //given
        String email = "email123@google.com";
        EmailVerificationCodeRequest request = new EmailVerificationCodeRequest(email, true);
        when(smtpService.sendEmailVerificationCode(request)).thenReturn(EMAIL_ALREADY_EXISTS);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post("/smtp/verification-code-transmission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
        EmailVerificationSendResult res = objectMapper.readValue(responseBodyAsString,EmailVerificationSendResult.class);

        //then
        assertThat(res).isEqualTo(EMAIL_ALREADY_EXISTS);
    }

    @DisplayName("인증 코드 검증 완료 시 정상 응답")
    @Test
    void verifyCodeTest_valid() throws Exception {
        //given
        String email = "email123@google.com";
        String joinCode = "123456";
        VerifyCodeRequest verifyCodeRequest = getVerifyCodeRequest(email, joinCode);
        when(smtpService.verifyCode(verifyCodeRequest)).thenReturn(true);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post("/smtp/code-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyCodeRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();

        //then
        assertThat(responseBodyAsString).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"naver.com", "hello.world", "this is invalid email address", ""})
    @DisplayName("인증 코드 검증 요쳥의 이메일 형식이 올바르지 않은 경우, 400(BAD_REQUEST) 예외 발생")
    void verifyCodeTest_whenInvalidEmailFormat_thenReturnBAD_REQUEST(String email) throws Exception {
        //given
        String joinCode = "123456";
        VerifyCodeRequest verifyCodeRequest = getVerifyCodeRequest(email, joinCode);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post("/smtp/code-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyCodeRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();

        //then
        assertThat(responseBodyAsString).contains(VALIDATION_FAILED_400.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1c121c", "12345", "1234567", "abcdef",""})
    @DisplayName("인증 코드 검증 코드 형식이 올바르지 않은 경우, 400(BAD_REQUEST) 예외 발생")
    void verifyCodeTest_whenInvalidJoinCodeFormat_thenReturnBAD_REQUEST(String joinCode) throws Exception {
        //given
        String email = "email123@google.com";
        VerifyCodeRequest verifyCodeRequest = getVerifyCodeRequest(email, joinCode);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post("/smtp/code-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyCodeRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();

        //then
        assertThat(responseBodyAsString).contains(VALIDATION_FAILED_400.name());
    }

    @DisplayName("인증 코드 불일치 시 400(BAD_REQUEST) 예외 발생")
    @Test
    void verifyCodeTest_whenIncorrectJoinCode_thenReturnBAD_REQUEST() throws Exception {
        //given
        String email = "email123@google.com";
        String invalidJoinCode = "123456";
        VerifyCodeRequest verifyCodeRequest = getVerifyCodeRequest(email, invalidJoinCode);
        when(smtpService.verifyCode(verifyCodeRequest)).thenReturn(false);

        //when && then
        mockMvc.perform(post("/smtp/code-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyCodeRequest)))
                .andExpect(status().isBadRequest());
    }

    VerifyCodeRequest getVerifyCodeRequest(String email, String code) {
        return new VerifyCodeRequest(email, code);
    }

}