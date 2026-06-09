package my.mma.api.global.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.CurrentFighterFightEventDto;
import my.mma.api.fightevent.dto.CurrentEventDto.FighterFightEventCardFighterDto;
import my.mma.api.global.ai.dto.AiQuestionDto;
import my.mma.api.global.ai.dto.CurrentFightEventChatTool;
import my.mma.api.global.ai.dto.FighterProfileChatTool;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.stream.dto.AiDto.AiChunkResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 챗봇 카테고리(fight/fighter)별로 필요한 데이터를 프롬프트에 직접 주입하고 LLM 응답을 스트리밍한다.
 * 답변은 cacheKey 기준으로 Redis 에 캐싱한다(히트 시 전체 답변을 이벤트 1개로 전송).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;
    private final AiAnswerCacheService cacheService;
    private final ObjectMapper objectMapper;
    private final RedisUtils<CurrentEventDto> redisUtils;
    private final FighterProfileQueryService fighterProfileQueryService;

    private static final String OUT_OF_SCOPE = "해당 질문은 서비스 범위를 벗어납니다.";

    private static final String COMMON_RULES = """
            너는 'fightweek' 앱의 MMA 분석 도우미다. 항상 한국어 존댓말(~합니다/~입니다체)로 답한다.

            [범위] '이번 주' 경기·출전 선수 분석만 다룬다.
            앱과 무관하거나 이번 주 카드와 무관한 주제는 "해당 질문은 서비스 범위를 벗어납니다."로 답한다.

            [데이터의 역할] 너는 일반 MMA 지식으로 추론하되, [데이터]는 그 추론에 쓰는 '최신·정확한 사실'이다.
            데이터가 추론을 대체하는 게 아니라, 네 추론을 최신 정보로 보정·증강한다.
            스타일 상성·상대해온 선수 수준·경기 IQ·내구력·과거 경기 경향 같은 정성적 판단엔 네 지식을 적극 활용하라.
            다만 사실(수치·전적·랭킹·대진·일정)이 네 기억과 충돌하면 [데이터]가 옳고,
            [데이터]에 없는 구체 수치·전적·결과를 사실처럼 지어내지는 마라. 값이 없으면 그 항목은 단정하지 마라.

            [지표 해석 주의] 커리어 스탯(SLpM 등)은 여러 신호 중 하나일 뿐이다. 상대해온 선수 수준이
            제각각이라 수치만으로 우열을 단정할 수 없다. 스타일·상대 수준·맥락과 종합해 확률적으로 판단하라.

            [careerStats 키 = 의미/단위]
            SLpM 분당 유효타격 수, SApM 분당 피격 수, tdAvg 15분당 테이크다운 수, subAvg 15분당 서브미션 시도 수 (모두 실수);
            strAcc 타격 정확도%, strDef 피격 방어%, tDAcc 테이크다운 정확도%, tdDef 테이크다운 방어% (모두 0~100 정수).

            [사용자 노출 규칙 — 중요] 위 키 이름(SLpM·strDef 등 내부 필드명)을 사용자에게 절대 그대로 쓰지 마라.
            "분당 유효타격 4.4회", "테이크다운 방어율 90%"처럼 평이한 한국어로 변환한다(heightCm·reachCm 등은 "신장 180cm"·"리치 188cm").
            어떤 항목의 값이 없으면 그 항목을 조용히 빼고 넘어가라. "제공되지 않았다/정보가 없다/데이터가 없어서/미상" 처럼
            데이터 부재를 사용자에게 알리는 표현은 절대 쓰지 마라('null'·'필드' 같은 시스템 용어도 금지).
            없는 정보는 일반 지식으로 자연스럽게 보완하되 단정하지 않는다.

            [응답 스타일] 결론 → 핵심 근거(수치 인용) → 불확실성 한 줄, 간결하게.
            답변은 질문에 대한 분석으로만 끝낸다. "다음엔 무엇을 알아봐 드릴까요?",
            "더 궁금한 점 있으신가요?", "원하시면 ~도 분석해 드릴게요" 같은 후속 질문 유도·되묻기·
            추가 제안 문장은 어떤 경우에도 덧붙이지 마라.
            """;

    /** 챗봇 진입 시 노출할 고정 질문 메뉴. */
    public List<AiQuestionDto> questions() {
        return Arrays.stream(AiQuestion.values()).map(AiQuestionDto::of).toList();
    }

    /**
     * 이벤트 단위 질문(가장 기대되는 경기/FOTN/피니시 확률 등)에 카드 전체를 context 로 주입해 답한다.
     */
    public Flux<ServerSentEvent<AiChunkResponse>> event(AiQuestion question) {
        if (question.getCategory() != AiCategory.EVENT) return outOfScope();
        CurrentEventDto event = currentEvent();
        if (event == null) return outOfScope();

        String cacheKey = "event:" + event.getId() + ":" + question.name();
        return stream(AiCategory.EVENT, buildCardContext(event), question.getPrompt(), cacheKey);
    }

    /**
     * 경기 단위 질문(A vs B)에 그 경기 두 선수만 context 로 주입해 답한다.
     * - 매주 대진이 바뀌므로 캐시 키에 fightId 를 포함시킨다.
     */
    public Flux<ServerSentEvent<AiChunkResponse>> fight(AiQuestion question, Long fightId) {
        if (question.getCategory() != AiCategory.FIGHT || fightId == null) return outOfScope();
        CurrentEventDto event = currentEvent();

        CurrentFighterFightEventDto ffe = event.getFighterFightEvents().stream()
                .filter(currFfe -> fightId.equals(currFfe.getId()))
                .findFirst().orElse(null);
        if (ffe == null) return outOfScope();

        String cacheKey = "fight:" + event.getId() + ":" + fightId + ":" + question.name();
        return stream(AiCategory.FIGHT, buildMatchupContext(ffe), question.getPrompt(), cacheKey);
    }

    /**
     * 이번 주 카드에 출전하는 선수 1명의 프로필을 주입해 스카우팅 리포트를 작성한다.
     * 카드에 없는 선수는 범위를 벗어난 것으로 처리한다.
     */
    public Flux<ServerSentEvent<AiChunkResponse>> fighter(AiQuestion question, Long fighterId) {
        if (question.getCategory() != AiCategory.FIGHTER || fighterId == null) return outOfScope();
        CurrentEventDto event = currentEvent();

        FighterFightEventCardFighterDto cardFighter = findCardFighter(event, fighterId);
        if (cardFighter == null) return outOfScope();  // 이번 주 카드에 없는 선수

        // 전적이 있으면 상세 프로필(스탯/최근전적), 없으면(신규 선수) 카드의 기본 프로필만으로 추론한다.
        FighterProfileChatTool profile = fighterProfileQueryService.getProfile(fighterId);
        String context = toJson(profile != null ? profile : FighterProfileChatTool.ofCard(cardFighter));

        String cacheKey = "fighter:" + event.getId() + ":" + fighterId + ":" + question.name();
        return stream(AiCategory.FIGHTER, context, question.getPrompt(), cacheKey);
    }

    // ===== context 조립 =====

    private CurrentEventDto currentEvent() {
        return redisUtils.getData(RedisKey.CURRENT_EVENT.getKey());
    }

    // 카드 개요 + 출전 선수 전원의 상세 프로필(커리어 스탯/최근 전적)을 한 번에 담는다.
    private String buildCardContext(CurrentEventDto event) {
        return toJson(new FightContext(CurrentFightEventChatTool.of(event),
                buildProfiles(cardFighterIds(event))));
    }

    // 특정 경기의 두 선수 프로필만 담는다.
    private String buildMatchupContext(CurrentFighterFightEventDto ffe) {
        Set<Long> ids = new LinkedHashSet<>();
        addId(ids, ffe.getWinner());
        addId(ids, ffe.getLoser());
        return toJson(new MatchupContext(ffe.getFightWeight(), ffe.isTitle(), buildProfiles(ids)));
    }

    private List<FighterProfileChatTool> buildProfiles(Set<Long> fighterIds) {
        List<FighterProfileChatTool> profiles = new ArrayList<>();
        for (Long id : fighterIds) {
            FighterProfileChatTool profile = fighterProfileQueryService.getProfile(id);
            if (profile != null) profiles.add(profile);
        }
        return profiles;
    }

    private Set<Long> cardFighterIds(CurrentEventDto event) {
        Set<Long> ids = new LinkedHashSet<>();
        for (CurrentFighterFightEventDto ffe : event.getFighterFightEvents()) {
            addId(ids, ffe.getWinner());
            addId(ids, ffe.getLoser());
        }
        return ids;
    }

    private void addId(Set<Long> ids, FighterFightEventCardFighterDto fighter) {
        ids.add(fighter.getId());
    }

    private FighterFightEventCardFighterDto findCardFighter(CurrentEventDto event, Long fighterId) {
        for (CurrentFighterFightEventDto ffe : event.getFighterFightEvents()) {
            if (fighterId.equals(ffe.getWinner().getId())) return ffe.getWinner();
            if (fighterId.equals(ffe.getLoser().getId())) return ffe.getLoser();
        }
        return null;
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI context 직렬화 실패", e);
        }
    }

    private record FightContext(CurrentFightEventChatTool card, List<FighterProfileChatTool> fighters) {}

    private record MatchupContext(String fightWeight, boolean title, List<FighterProfileChatTool> fighters) {}

    // ===== 스트리밍 + 캐싱 =====

    private Flux<ServerSentEvent<AiChunkResponse>> stream(AiCategory category, String context,
                                                          String question, String cacheKey) {
        // 캐시 히트: 저장된 전체 답변을 이벤트 1개로 전송한다(가짜 청크 분할 없이 바로 노출).
        String cached = cacheService.get(cacheKey);
        if (cached != null) {
            return Flux.just(token(cached), done());
        }

        String system = COMMON_RULES + "\n" + category.getPromptSection() + "\n[데이터]\n" + context;

        // tool calling 제거로 OpenAI 호출은 1라운드지만, usage 청크가 반복될 수 있어 변할 때만 합산한다.
        AtomicInteger inSum = new AtomicInteger();
        AtomicInteger outSum = new AtomicInteger();
        AtomicReference<String> lastUsageKey = new AtomicReference<>("");
        // 캐시 미스: 실시간 스트리밍하면서 토큰을 누적했다가 완료 시 1회 저장한다.
        StringBuilder answerBuf = new StringBuilder();

        return chatClient.prompt()
                .system(system)
                .user(question)
                .stream()
                .chatResponse()
                .doOnNext(resp -> {
                    Usage usage = resp.getMetadata().getUsage();
                    if (usage == null || usage.getTotalTokens() == null || usage.getTotalTokens() == 0) return;
                    int in = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                    int out = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                    String key = in + ":" + out;
                    if (!key.equals(lastUsageKey.getAndSet(key))) {
                        inSum.addAndGet(in);
                        outSum.addAndGet(out);
                    }
                })
                .doOnComplete(() -> {
                    log.info("[AI 토큰] category={} in={} out={} total={}",
                            category, inSum.get(), outSum.get(), inSum.get() + outSum.get());
                    cacheService.save(cacheKey, answerBuf.toString());
                })
                .mapNotNull(resp -> {
                    // 텍스트 없는 청크(usage/finishReason 전용)는 getText()=null →
                    // 그대로 내보내면 클라이언트에 'null' 문자열로 찍힌다. 이런 청크는 방출하지 않는다.
                    if (resp.getResult() == null || resp.getResult().getOutput() == null) return null;
                    String chunk = resp.getResult().getOutput().getText();
                    if (chunk == null || chunk.isEmpty()) return null;
                    answerBuf.append(chunk);
                    return token(chunk);
                })
                .concatWith(Mono.just(done()))
                .onErrorResume(e -> Mono.just(
                        ServerSentEvent.builder(new AiChunkResponse("처리 중 오류가 발생했습니다."))
                                .event("error").build()));
    }

    private Flux<ServerSentEvent<AiChunkResponse>> outOfScope() {
        return Flux.just(token(OUT_OF_SCOPE), done());
    }

    private ServerSentEvent<AiChunkResponse> token(String text) {
        return ServerSentEvent.builder(new AiChunkResponse(text)).event("token").build();
    }

    private ServerSentEvent<AiChunkResponse> done() {
        return ServerSentEvent.builder(new AiChunkResponse("done")).event("done").build();
    }

}
