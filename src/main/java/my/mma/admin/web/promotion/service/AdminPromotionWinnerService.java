package my.mma.admin.web.promotion.service;

import lombok.RequiredArgsConstructor;
import my.mma.admin.web.promotion.dto.AdminWinnerResponse;
import my.mma.api.event.promotion.entity.SendStatus;
import my.mma.api.event.promotion.repository.PromotionWinnerRepository;
import my.mma.api.global.s3.service.S3ImgService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자: 당첨자 조회 + 일괄 발송 오케스트레이션.
 * 실제 개별 발송(건별 트랜잭션)은 {@link GifticonMailSender}가 담당.
 */
@Service
@RequiredArgsConstructor
public class AdminPromotionWinnerService {

    private final PromotionWinnerRepository promotionWinnerRepository;
    private final S3ImgService s3ImgService;
    private final GifticonMailSender gifticonMailSender;

    /** 대시보드용 당첨자 목록 (쿠폰 이미지는 관리자 확인용 presigned URL) */
    @Transactional(readOnly = true)
    public List<AdminWinnerResponse> getWinners(Long promotionId) {
        return promotionWinnerRepository.findForAdminByPromotionId(promotionId).stream()
                .map(w -> AdminWinnerResponse.of(w,
                        s3ImgService.generateGifticonImgUrl(w.getGifticon().getImageKey())))
                .toList();
    }

    /** 미발송(PENDING/FAILED) 일괄 발송 — 각 건은 GifticonMailSender.send가 독립 트랜잭션으로 커밋 */
    public void sendAll(Long promotionId) {
        promotionWinnerRepository.findUnsentWinnerIdsByPromotionId(promotionId, SendStatus.SENT)
                .forEach(gifticonMailSender::send);
    }
}
