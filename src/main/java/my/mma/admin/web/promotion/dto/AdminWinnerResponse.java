package my.mma.admin.web.promotion.dto;

import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.PromotionWinner;
import my.mma.api.event.promotion.entity.SendStatus;
import my.mma.api.user.entity.User;

/**
 * 관리자 대시보드 당첨자 행.
 * couponImageUrl은 발송 확인용 presigned URL (관리자 화면 한정 — 공개 X).
 */
public record AdminWinnerResponse(
        Long winnerId,
        String nickname,
        String email,
        String gifticonName,
        String couponNumber,
        String couponImageUrl,
        SendStatus sendStatus
) {
    public static AdminWinnerResponse of(PromotionWinner winner, String couponImageUrl) {
        User user = winner.getWinner();
        Gifticon gifticon = winner.getGifticon();
        return new AdminWinnerResponse(
                winner.getId(),
                user.getNickname(),
                user.getEmail(),
                gifticon.getName(),
                gifticon.getCouponNumber(),
                couponImageUrl,
                winner.getSendStatus());
    }
}
