package my.mma.api.event.promotion.service;

import lombok.RequiredArgsConstructor;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.event.promotion.constant.PromotionPolicy;
import my.mma.api.event.promotion.dto.PromotionDetailDto;
import my.mma.api.event.promotion.dto.PromotionDetailResponse;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로모션 상세 조회 조립.
 * 유저 공유 데이터(PromotionCacheService, 캐시)에 로그인 유저별 응모 현황을 합쳐 반환한다.
 * 응모 수는 유저별이라 캐시할 수 없어(취소/추가 시 잦은 변경) 매 요청 BetCard COUNT를 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionDetailService {

    private final PromotionCacheService promotionCacheService;
    private final BetCardRepository betCardRepository;
    private final UserRepository userRepository;

    public PromotionDetailResponse getDetail(String email, Long promotionId) {
        PromotionDetailDto detail = promotionCacheService.getPromotionDetail(promotionId);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));

        int count = betCardRepository.countByUserIdAndDuration(
                user.getId(), detail.startDate(), detail.endDate());
        int entryCount = Math.min(count, PromotionPolicy.ENTRY_CAP);
        return new PromotionDetailResponse(detail, entryCount, PromotionPolicy.ENTRY_CAP);
    }
}
