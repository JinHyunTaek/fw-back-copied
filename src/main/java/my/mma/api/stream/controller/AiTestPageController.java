package my.mma.api.stream.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [TEST 전용] LLM 스트리밍 응답을 브라우저에서 확인하기 위한 Thymeleaf 페이지 서빙.
 * StreamController 는 @RestController 라 뷰를 반환할 수 없어 분리.
 */
@Controller
public class AiTestPageController {

    @GetMapping("/stream/ai/test")
    public String testPage() {
        return "ai/test";
    }
}
