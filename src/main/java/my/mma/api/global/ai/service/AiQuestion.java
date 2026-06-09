package my.mma.api.global.ai.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static my.mma.api.global.ai.service.AiCategory.EVENT;
import static my.mma.api.global.ai.service.AiCategory.FIGHT;
import static my.mma.api.global.ai.service.AiCategory.FIGHTER;

/**
 * 사용자에게 노출되는 고정 질문 카탈로그.
 * 질문 텍스트(LLM user 메시지)·카테고리·노출 라벨을 서버가 단일 출처로 관리한다.
 * 클라이언트는 enum 이름만 전송하므로(화이트리스트), 문구 수정·질문 추가가 서버 배포만으로 반영된다.
 * <p>
 * 카테고리가 곧 필요 파라미터를 결정한다: EVENT=없음, FIGHT=fightId, FIGHTER=fighterId.
 */
@Getter
@RequiredArgsConstructor
public enum AiQuestion {

    // 이벤트 단위 (id 불필요) - 카드 전체를 비교/큐레이션
    MOST_ANTICIPATED(EVENT, "가장 기대되는 경기",
            "이번 주 카드에서 가장 기대되는 경기를 하나 골라 근거와 함께 설명해줘."),
    LIKELY_FINISH(EVENT, "피니시 확률 높은 경기",
            "이번 주 카드에서 KO·서브미션 등 피니시로 끝날 가능성이 높은 경기를 골라 설명해줘."),
    FIGHT_OF_THE_NIGHT(EVENT, "파이트 오브 더 나잇 예상",
            "이번 주 카드에서 파이트 오브 더 나잇 후보를 골라 이유와 함께 설명해줘."),
    PERFORMANCE_OF_THE_NIGHT(EVENT, "퍼포먼스 오브 더 나잇 예상",
            "이번 주 카드에서 퍼포먼스 오브 더 나잇 후보를 골라 이유와 함께 설명해줘."),

    // 경기 단위 (fightId 필요) - 두 선수 head-to-head
    PREDICT_WINNER(FIGHT, "승자 예측",
            "이 경기의 승자를 확률적으로 예측하고 근거를 설명해줘."),
    FINISH_METHOD(FIGHT, "피니시 방식 예측",
            "이 경기가 KO/TKO·서브미션·판정 중 어떤 방식으로 끝날 가능성이 높은지 예측해줘."),
    MATCHUP_BREAKDOWN(FIGHT, "경기 양상 분석",
            "이 경기의 스타일 매치업과 예상 전개를 분석해줘."),

    // 선수 단위 (fighterId 필요) - 스카우팅 리포트
    SCOUTING_REPORT(FIGHTER, "선수 스카우팅 리포트",
            "이 선수의 체격·파이팅 스타일·강점·최근 폼을 스카우팅 리포트로 작성해줘.");

    private final AiCategory category;
    private final String label;
    private final String prompt;
}
