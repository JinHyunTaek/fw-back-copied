package my.mma.api.smtp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.security.entity.PasswordResetToken;
import my.mma.api.security.repository.PasswordResetTokenRepository;
import my.mma.api.smtp.constant.JoinCodeConstant;
import my.mma.api.smtp.dto.EmailVerificationCodeRequest;
import my.mma.api.smtp.dto.PasswordResetTokenResponse;
import my.mma.api.smtp.dto.VerifyCodeRequest;
import my.mma.api.smtp.entity.JoinCode;
import my.mma.api.smtp.repository.JoinCodeRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.smtp.dto.EmailVerificationSendResult;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

import static my.mma.api.smtp.dto.EmailVerificationSendResult.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmtpService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final JoinCodeRepository joinCodeRepository;
    private final PasswordResetTokenRepository resetTokenRepository;

    @Transactional
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
        sendCode(request);
        return SUCCESS;
    }

    private void sendCode(EmailVerificationCodeRequest request) {
        String joinCode = generateRandomNumber();
        SimpleMailMessage smm = new SimpleMailMessage();
        smm.setTo(request.email());
        smm.setSubject("[fightweek] 이메일 인증 코드 안내");
        smm.setText(
                "아래 인증 코드를 입력하여 인증을 완료해주세요.\n" + "인증 코드:" + joinCode);
        mailSender.send(smm);
        joinCodeRepository.save(JoinCode.builder()
                .email(request.email())
                .code(joinCode)
                .expiration(JoinCodeConstant.EXPIRATION_SECONDS.getValue())
                .build());
    }

    private String generateRandomNumber() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    @Transactional
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

    @Transactional
    public PasswordResetTokenResponse createPasswordResetToken(String email) {
        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .email(email)
                .build());
        return new PasswordResetTokenResponse(token);
    }

}
