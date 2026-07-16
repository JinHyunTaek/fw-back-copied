package my.mma.api.smtp.service;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기프티콘 메일 발송 검증 — 실 SMTP/앱 컨텍스트 없이 페이크 SMTP(GreenMail)로
 * "HTML 본문 + 인라인 이미지(cid)"가 실제 MIME으로 조립·전송되는지 확인한다.
 * (GifticonMailSender가 사용하는 SmtpService.sendHtmlWithInlineImage 를 그대로 검증)
 */
class SmtpServiceEmailTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private SmtpService smtpService() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(greenMail.getSmtp().getPort());
        sender.setDefaultEncoding("UTF-8");
        return new SmtpService(sender);
    }

    @Test
    @DisplayName("HTML + 인라인 이미지 메일이 실제 MIME(multipart + cid 파트)으로 전송된다")
    void sendHtmlWithInlineImage_deliversMultipartWithInlineCid() throws Exception {
        // given: GifticonMailSender.sendMail 이 만드는 것과 동일한 형태의 입력
        String html = """
                <h2>축하합니다! 당첨되셨어요 🎉</h2>
                <p>상품: <b>BBQ 황금올리브</b><br/>
                쿠폰번호: <b>ABCD-1234-EFGH</b><br/>
                유효기간: 2026-08-11</p>
                <img src="cid:gifticon" style="max-width:320px"/>
                """;
        byte[] fakePng = new byte[]{ (byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

        // when
        smtpService().sendHtmlWithInlineImage(
                "winner@test.com", "[파이트위크] 프로모션 당첨을 축하합니다! 🎁",
                html, "gifticon", fakePng, "image/png");

        // then
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage msg = greenMail.getReceivedMessages()[0];

        assertThat(msg.getSubject()).contains("당첨을 축하");
        assertThat(msg.getAllRecipients()[0].toString()).isEqualTo("winner@test.com");
        assertThat(msg.getContentType()).containsIgnoringCase("multipart");

        InlineParts parts = collect((Multipart) msg.getContent());
        assertThat(parts.html)
                .contains("BBQ 황금올리브")          // 상품명
                .contains("ABCD-1234-EFGH")           // 쿠폰번호
                .contains("cid:gifticon");            // 인라인 이미지 참조
        assertThat(parts.hasInlineImage).isTrue();    // Content-ID 로 첨부된 이미지 파트 존재
    }

    // 멀티파트를 재귀 순회해 html 본문과 인라인 이미지 파트 유무를 수집
    private InlineParts collect(Multipart multipart) throws Exception {
        InlineParts result = new InlineParts();
        walk(multipart, result);
        return result;
    }

    private void walk(Multipart multipart, InlineParts acc) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            Part part = multipart.getBodyPart(i);
            Object content = part.getContent();
            if (content instanceof Multipart nested) {
                walk(nested, acc);
            } else if (part.isMimeType("text/html")) {
                acc.html = new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else if (part.getContentType() != null
                    && part.getContentType().toLowerCase().startsWith("image/")) {
                acc.hasInlineImage = true;
            }
        }
    }

    private static class InlineParts {
        String html = "";
        boolean hasInlineImage = false;
    }
}
