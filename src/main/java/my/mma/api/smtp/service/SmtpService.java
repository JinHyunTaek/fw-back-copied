package my.mma.api.smtp.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 범용 메일 발송기 (인프라).
 * 도메인/기능에 무관한 원시값(to/subject/body 등)만 받는다. 제목·본문 조립은 각 기능 서비스가 담당.
 */
@Service
@RequiredArgsConstructor
public class SmtpService {

    private final JavaMailSender mailSender;

    /** 텍스트 메일 */
    public void sendText(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    /** HTML 메일 + 본문 인라인 이미지(cid). html 안의 {@code <img src="cid:{cid}">}와 cid가 일치해야 렌더된다. */
    public void sendHtmlWithInlineImage(String to, String subject, String html,
                                        String cid, byte[] image, String contentType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // multipart
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // html
            helper.addInline(cid, new ByteArrayResource(image), contentType);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new CustomException(ErrorCode.SERVER_ERROR_500, "메일 발송 실패");
        }
    }

}
