package my.mma.api.global.ai.dto;

import my.mma.api.global.ai.service.AiQuestion;

/**
 * 챗봇 진입 시 클라이언트에 내려주는 질문 메뉴 항목.
 * category 가 곧 필요 파라미터를 결정한다: EVENT=없음, FIGHT=fightId, FIGHTER=fighterId.
 */
public record AiQuestionDto(
        String value,    // AiQuestion enum 이름 (요청 시 그대로 전송)
        String label,    // 사용자 노출 라벨
        String category  // EVENT / FIGHT / FIGHTER
) {
    public static AiQuestionDto of(AiQuestion q) {
        return new AiQuestionDto(q.name(), q.getLabel(), q.getCategory().name());
    }
}
