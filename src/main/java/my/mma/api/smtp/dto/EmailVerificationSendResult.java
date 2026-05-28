package my.mma.api.smtp.dto;

public enum EmailVerificationSendResult {
    SUCCESS,
    EMAIL_ALREADY_EXISTS,
    EMAIL_NOT_FOUND,
    SOCIAL_LOGIN_ACCOUNT
}
