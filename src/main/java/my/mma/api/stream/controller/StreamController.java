package my.mma.api.stream.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.ai.dto.AiQuestionDto;
import my.mma.api.global.ai.service.AiChatService;
import my.mma.api.global.ai.service.AiQuestion;
import my.mma.api.stream.dto.AiDto.AiChunkResponse;
import my.mma.api.stream.service.StreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;
    private final AiChatService aiChatService;

    // stream room 진입 시 최초 1회만 호출 (이후에는 socket을 통한 n분 주기의 broadcast 응답)
    @GetMapping("/weekly_event")
    public ResponseEntity<CurrentEventDto> weeklyEvent(
    ) {
        return ResponseEntity.ok().body(streamService.getWeeklyEvent());
    }

    // 챗봇 진입 시 노출할 고정 질문 메뉴 (클라이언트가 버튼/리스트 렌더에 사용)
    @GetMapping("/ai/questions")
    public ResponseEntity<List<AiQuestionDto>> questions() {
        return ResponseEntity.ok(aiChatService.questions());
    }

    // 브라우저 EventSource는 GET만 + body 불가 → @RequestParam 으로 받음.
    // question: AiQuestion enum 값. category 일치/id 검증은 서비스에서 수행.

    // 이벤트 단위 질문 (가장 기대되는 경기/FOTN/피니시 확률 등) - id 불필요
    @GetMapping(value = "/ai/event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AiChunkResponse>> event(@RequestParam AiQuestion question) {
        return aiChatService.event(question);
    }

    // 경기 단위 질문 (A vs B 분석/승자/양상) - fightId 필수
    @GetMapping(value = "/ai/fight", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AiChunkResponse>> fight(@RequestParam AiQuestion question,
                                                        @RequestParam Long fightId) {
        return aiChatService.fight(question, fightId);
    }

    // 선수 단위 질문 (스카우팅 리포트) - fighterId 필수
    @GetMapping(value = "/ai/fighter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AiChunkResponse>> fighter(@RequestParam AiQuestion question,
                                                          @RequestParam Long fighterId) {
        return aiChatService.fighter(question, fighterId);
    }

}
