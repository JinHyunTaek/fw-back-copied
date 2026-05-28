package my.mma.api.bet.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PredictionPolicy {

    ENTRY_FEE(300),
    WINNER_HIT(300),
    DRAW_HIT(3000),
    WIN_METHOD_FINISH_HIT(300),
    WIN_METHOD_DEC_HIT(200),
    FINISH_ROUND_HIT(300),
    OTN_HIT(300),
    FULL_OPTION_BONUS(200),
    // 조합 예측 시 각 카드 옵션당 + 100
    COMBO_BONUS_PER_OPTION(100);

    private final int point;

}
