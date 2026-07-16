package my.mma.api.smtp.service;

import my.mma.api.exception.CustomException;
import my.mma.api.security.repository.PasswordResetTokenRepository;
import my.mma.api.smtp.dto.EmailVerificationCodeRequest;
import my.mma.api.smtp.dto.VerifyCodeRequest;
import my.mma.api.smtp.entity.JoinCode;
import my.mma.api.smtp.repository.JoinCodeRepository;
import my.mma.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static my.mma.api.exception.ErrorCode.NO_SUCH_EMAIL_CONFIGURED_400;
import static my.mma.api.smtp.constant.JoinCodeConstant.EXPIRATION_SECONDS;
import static my.mma.api.smtp.dto.EmailVerificationSendResult.EMAIL_ALREADY_EXISTS;
import static my.mma.api.smtp.dto.EmailVerificationSendResult.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserVerificationServiceTest {

    @Mock
    private JoinCodeRepository joinCodeRepository;
    @Mock
    private SmtpService smtpService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @InjectMocks
    private UserVerificationService userVerificationService;

    @DisplayName("아직 가입되지 않은 이메일 계정으로 sendEmailVerificationCode() 요청 시 SUCCESS 반환")
    @Test
    void sendJoinCode() {
        //given
        String email = "email123@google.com";
        EmailVerificationCodeRequest request = new EmailVerificationCodeRequest(email, true);
        when(userRepository.existsByEmail(email)).thenReturn(false);

        //when && then
        assertThat(userVerificationService.sendEmailVerificationCode(request)).isEqualTo(SUCCESS);
    }

    @DisplayName("이미 회원가입된 이메일 계정으로 sendEmailVerificationCode() 요청 시 EMAIL_ALREADY_EXISTS 반환")
    @Test
    void sendJoinCode_whenEmailAlreadyExists_ThenReturnEMAIL_ALREADY_EXISTS() {
        //given
        String email = "email123@naver.com";
        EmailVerificationCodeRequest request = new EmailVerificationCodeRequest(email, true);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        //when && then
        assertThat(userVerificationService.sendEmailVerificationCode(request)).isEqualTo(EMAIL_ALREADY_EXISTS);
    }

    @DisplayName("서버로부터 받은 smtp 코드 올바르게 입력한 경우, verifyCode() 호출 시 true 반환")
    @Test
    void verifyCode_whenJoinCodeCorrect() {
        //given
        String email = "email123@google.com";
        String joinCode = "123456";
        JoinCode joinCodeToVerify = getJoinCode(email, joinCode);

        when(joinCodeRepository.findById(email)).thenReturn(ofNullable(joinCodeToVerify));

        // when
        boolean isVerified = userVerificationService.verifyCode(new VerifyCodeRequest(email, joinCode));

        // then
        verify(joinCodeRepository).delete(joinCodeToVerify);
        assertThat(isVerified).isEqualTo(true);
    }

    @DisplayName("서버로부터 받은 smtp 코드 잘 못 입력한 경우, verifyCode() 호출 시 false 반환")
    @Test
    void verifyCode_whenJoinCodeIncorrect() {
        //given
        String email = "email123@google.com";
        String joinCode = "123456";
        String invalidJoinCode = "654321";
        JoinCode joinCodeToVerify = getJoinCode(email, invalidJoinCode);

        when(joinCodeRepository.findById(email)).thenReturn(ofNullable(joinCodeToVerify));

        // when
        boolean isVerified = userVerificationService.verifyCode(new VerifyCodeRequest(email, joinCode));

        // then
        verify(joinCodeRepository, times(0)).delete(joinCodeToVerify);
        assertThat(isVerified).isEqualTo(false);
    }

    @DisplayName("repository에 없는 이메일로 verifyCode() 호출 시 NO_SUCH_EMAIL_CONFIGURED_400 예외 발생")
    @Test
    void verifyCode_NO_SUCH_EMAIL_CONFIGURED_400_whenEmailNotFound() {
        //given
        String wrongEmail = "email123@naver.com";
        String joinCode = "123456";

        // when && then
        when(joinCodeRepository.findById(wrongEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userVerificationService.verifyCode(new VerifyCodeRequest(wrongEmail, joinCode)))
                .isInstanceOf(CustomException.class)
                .hasMessage(NO_SUCH_EMAIL_CONFIGURED_400.name());
    }

    private JoinCode getJoinCode(String email, String joinCode) {
        return JoinCode.builder()
                .email(email)
                .code(joinCode)
                .expiration(EXPIRATION_SECONDS.getValue())
                .build();
    }

}
