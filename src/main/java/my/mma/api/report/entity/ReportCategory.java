package my.mma.api.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportCategory {
    SWEAR_WORD("욕설"),
    THREAT("위협/협박"),
    WRONG_INFO("잘못된 정보 유출"),
    IMPROPER_NICKNAME("부적절한 닉네임"),
    IMPROPER_IMAGE("부적절한 프로필 사진"),
    PERSONAL_INFO_LEAK("개인정보 유출"),
    SEXUAL_WORD("성적인 발언"),
    EXCESSIVE_COMMENT("댓글 도배"),
    OTHER("기타");

    private final String description;

}
