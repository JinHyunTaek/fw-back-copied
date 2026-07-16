package my.mma.api.event.promotion.constant;

/**
 * 프로모션 응모 정책 상수.
 * 예측 1회 = 응모 1회, 유저당 응모 상한은 ENTRY_CAP.
 * 추첨(PromotionDrawService)과 상세 조회(PromotionDetailService) 양쪽에서 참조한다.
 */
public final class PromotionPolicy {

    private PromotionPolicy() {}

    public static final int ENTRY_CAP = 5;
}
