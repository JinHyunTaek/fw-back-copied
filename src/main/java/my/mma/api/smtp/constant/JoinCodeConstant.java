package my.mma.api.smtp.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum JoinCodeConstant {

    NUMBER_COUNT(6),
    NUMBER_RANGE(10),
    EXPIRATION_SECONDS(300);

    private final int value;

}
