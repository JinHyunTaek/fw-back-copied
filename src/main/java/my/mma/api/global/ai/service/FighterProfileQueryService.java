package my.mma.api.global.ai.service;

import lombok.RequiredArgsConstructor;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.global.ai.dto.FighterProfileChatTool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 선수 프로필(커리어 스탯/최근 전적) 조회를 선수 단위로 캐싱한다.
 * 카드 단위 질문은 출전 선수 수만큼 findByFighterName 가 호출되므로, 결과 DTO 를 캐싱해
 * 같은 이벤트의 다른 질문/엔드포인트에서 재사용한다.
 * - 엔티티 그래프가 아닌 record DTO 를 캐싱하므로 직렬화가 안전하다.
 * - self-invocation 시 @Cacheable 프록시가 동작하지 않으므로 AiChatService 와 별도 빈으로 둔다.
 */
@Service
@RequiredArgsConstructor
public class FighterProfileQueryService {

    private final FighterFightEventRepository fighterFightEventRepository;

    @Cacheable(cacheNames = "fighterProfile", key = "#fighterId")
    public FighterProfileChatTool getProfile(Long fighterId) {
        return FighterProfileChatTool.of(fighterId, fighterFightEventRepository.findByFighterId(fighterId));
    }

}
