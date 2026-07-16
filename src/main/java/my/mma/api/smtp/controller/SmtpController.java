package my.mma.api.smtp.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.smtp.dto.EmailVerificationCodeRequest;
import my.mma.api.smtp.dto.EmailVerificationSendResult;
import my.mma.api.smtp.dto.PasswordResetTokenResponse;
import my.mma.api.smtp.dto.VerifyCodeRequest;
import my.mma.api.smtp.service.UserVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/smtp")
public class SmtpController {

    private final UserVerificationService userVerificationService;

    @PostMapping("/verification-code-transmission")
    public ResponseEntity<EmailVerificationSendResult> sendEmailVerificationCode(
            @RequestBody @Validated EmailVerificationCodeRequest request
    ) {
        return ResponseEntity.ok().body(userVerificationService.sendEmailVerificationCode(request));
    }

    @PostMapping("/code-verification")
    public ResponseEntity<Void> verifyCode(
            @RequestBody @Validated VerifyCodeRequest verifyCodeRequest
    ) {
        if (userVerificationService.verifyCode(verifyCodeRequest))
            return ResponseEntity.status(HttpStatus.OK).body(null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @PostMapping("/password-reset-token")
    public ResponseEntity<PasswordResetTokenResponse> verifyCodeAndCreateResetToken(
            @RequestBody @Validated VerifyCodeRequest verifyCodeRequest
    ) {
        if (!userVerificationService.verifyCode(verifyCodeRequest))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        PasswordResetTokenResponse response = userVerificationService.createPasswordResetToken(verifyCodeRequest.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
