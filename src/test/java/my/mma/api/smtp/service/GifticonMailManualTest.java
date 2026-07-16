package my.mma.api.smtp.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인라인 이미지 메일 렌더링 수동 스모크 테스트 (자동 빌드 제외 — @Disabled).
 *
 * 메일 자동설정만 로드(S3/JPA/AWS/Redis 로딩 X) → 컨텍스트 가볍고 AWS 자격증명 불필요.
 * 이미지 렌더링 확인엔 S3가 필요 없어 로컬 png로 검증(S3 fetch는 별개 관심사).
 *
 * 메일 host/port/username/password : application.yml (spring.mail.*, gitignored) → 자동
 * 받는 사람/이미지 경로            : Run Config Environment variables 로 주입 (커밋 X)
 *
 * 실행: @Disabled 주석 처리 + Run Config Environment variables 에
 *       TEST_MAIL_TO=...;TEST_IMAGE_PATH=/절대/경로.png  세팅 후 단건 실행.
 */
@Disabled("수동 실행 전용: 실제 Gmail 발송 → 받은편지함에서 인라인 이미지 확인")
@SpringBootTest(classes = MailSenderAutoConfiguration.class) // 메일 자동설정만
class GifticonMailManualTest {

    @Autowired
    JavaMailSender mailSender; // application.yml의 spring.mail.* 로 구성된 실제 빈

    @Test
    void 인라인_이미지_메일_렌더링_확인() throws Exception {
        String mailTo = System.getenv("TEST_MAIL_TO");
        String imagePath = System.getenv("TEST_IMAGE_PATH");
        assertThat(mailTo).as("Run Config Environment variables에 TEST_MAIL_TO 설정 필요").isNotBlank();
        assertThat(imagePath).as("Run Config Environment variables에 TEST_IMAGE_PATH 설정 필요").isNotBlank();

        byte[] image = Files.readAllBytes(Path.of(imagePath));
        String html = """
                <h2>축하합니다! 🎉</h2>
                <p>상품: 스타벅스 아메리카노 / 쿠폰번호: TEST-123</p>
                <img src="cid:gifticon" style="max-width:320px"/>
                """;

        new SmtpService(mailSender).sendHtmlWithInlineImage(
                mailTo,
                "[fightweek] 인라인 이미지 발송 테스트",
                html, "gifticon", image, "image/jpeg"); // 테스트 이미지가 .jpg (png면 image/png)
    }
}
