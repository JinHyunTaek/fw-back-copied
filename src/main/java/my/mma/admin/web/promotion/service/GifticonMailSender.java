package my.mma.admin.web.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.PromotionWinner;
import my.mma.api.event.promotion.repository.PromotionWinnerRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.smtp.service.SmtpService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기프티콘 개별 발송 단위.
 * 별도 빈으로 분리해, 일괄 발송 루프에서 호출해도 @Transactional이 프록시 경유로 정상 적용된다(건별 커밋).
 * 실제 메일 발송은 범용 SmtpService에 위임 — 여기서는 "기프티콘 메일 내용" 조립 + 상태 전이만.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GifticonMailSender {

    private final PromotionWinnerRepository promotionWinnerRepository;
    private final S3ImgService s3ImgService;
    private final SmtpService smtpService;

    /** 개별 발송: 성공 SENT / 실패 FAILED / 이미 SENT면 스킵(멱등). 건별 트랜잭션. */
    @Transactional
    public void send(Long winnerId) {
        PromotionWinner winner = promotionWinnerRepository.findById(winnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        if (winner.isSent()) return;
        try {
            sendMail(winner);
            winner.markSent();
        } catch (Exception e) {
            winner.markFailed();
            log.error("[발송] 실패 winnerId={}", winnerId, e);
        }
    }

    private void sendMail(PromotionWinner winner) {
        Gifticon gifticon = winner.getGifticon();
        byte[] image = s3ImgService.getGifticonImageBytes(gifticon.getImageKey());
        String html = """
                <h2>축하합니다! 당첨되셨어요 🎉</h2>
                <p>상품: <b>%s</b><br/>
                쿠폰번호: <b>%s</b><br/>
                유효기간: %s</p>
                <img src="cid:gifticon" style="max-width:320px"/>
                """.formatted(gifticon.getName(), gifticon.getCouponNumber(), gifticon.getExpiryDate());

        smtpService.sendHtmlWithInlineImage(
                winner.getWinner().getEmail(),
                "[파이트위크] 프로모션 당첨을 축하합니다! 🎁",
                html, "gifticon", image, contentTypeOf(gifticon.getImageKey()));
    }

    // imageKey 확장자로 contentType 결정 (업로드 시 UUID + 확장자로 저장됨)
    private String contentTypeOf(String imageKey) {
        String lower = imageKey.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream"; // 확장자 없는 레거시 키 폴백
    }
}
