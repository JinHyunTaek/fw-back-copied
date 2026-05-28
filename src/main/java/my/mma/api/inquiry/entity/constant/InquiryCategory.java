package my.mma.api.inquiry.entity.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum InquiryCategory {
    ERROR("에러"),
    USAGE("사용법"),
    FEEDBACK("건의사항"),
    OTHER("기타");

    private final String description;
}
