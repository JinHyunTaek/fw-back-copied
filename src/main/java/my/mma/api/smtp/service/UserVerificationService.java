package my.mma.api.smtp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.security.entity.PasswordResetToken;
import my.mma.api.security.repository.PasswordResetTokenRepository;
import my.mma.api.smtp.dto.EmailVerificationCodeRequest;
import my.mma.api.smtp.dto.EmailVerificationSendResult;
import my.mma.api.smtp.dto.PasswordResetTokenResponse;
import my.mma.api.smtp.dto.VerifyCodeRequest;
import my.mma.api.smtp.entity.JoinCode;
import my.mma.api.smtp.repository.JoinCodeRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

import static my.mma.api.smtp.dto.EmailVerificationSendResult.*;
import static my.mma.api.smtp.dto.EmailVerificationSendResult.SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVerificationService {

    private final UserRepository userRepository;
    private final SmtpService smtpService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final JoinCodeRepository joinCodeRepository;

    @Transactional(readOnly = true)
    public EmailVerificationSendResult sendEmailVerificationCode(EmailVerificationCodeRequest request) {
        if (request.isJoin()) {
            if (userRepository.existsByEmail(request.email()))
                return EMAIL_ALREADY_EXISTS;
        } else {
            User user = userRepository.findByEmail(request.email()).orElse(null);
            if (user == null) {
                return EMAIL_NOT_FOUND;
            } else if (user.getPassword() == null) {
                return SOCIAL_LOGIN_ACCOUNT;
            }
        }
        String code = generateRandomNumber();
        joinCodeRepository.save(JoinCode.of(request.email(), code));
        smtpService.sendText(request.email(),
                "[파이트위크] 이메일 인증 코드 안내",
                "아래 인증 코드를 입력하여 인증을 완료해주세요.\n인증 코드:" + code);
        return SUCCESS;
    }

    private String generateRandomNumber() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    public boolean verifyCode(VerifyCodeRequest verifyCodeDto) {
        JoinCode joinCode = joinCodeRepository.findById(verifyCodeDto.email()).orElseThrow(
                () -> new CustomException(ErrorCode.NO_SUCH_EMAIL_CONFIGURED_400)
        );
        if (joinCode.getCode().equals(verifyCodeDto.code())) {
            joinCodeRepository.delete(joinCode);
            return true;
        }
        return false;
    }

    public PasswordResetTokenResponse createPasswordResetToken(String email) {
        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .email(email)
                .build());
        return new PasswordResetTokenResponse(token);
    }

}
