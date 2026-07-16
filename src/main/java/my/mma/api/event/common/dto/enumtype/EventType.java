package my.mma.api.event.common.dto.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {

    PROMOTION("프로모션"),
    REFERRAL("친구 초대"),
    POINT_BURNING("포인트 버닝");

    private final String description;

}
