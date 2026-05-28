package my.mma.api.game.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.game.dto.*;
import my.mma.api.game.service.*;
import my.mma.api.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game")
public class GameController {

    private final List<GameService<?>> gameServices;
    private final GamePointService gamePointService;

    @GetMapping("/start")
    public ResponseEntity<List<?>> getNameGameQuestions(
            @RequestParam("type") String type,
            @RequestParam("isNormal") boolean isNormal
    ) {
        GameService<?> service = gameServices.stream()
                .filter(s -> s.getType().name().equals(type))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        return ResponseEntity.ok().body(service.generateGame(isNormal));
    }

    @GetMapping("/attempt_count")
    public ResponseEntity<GameAttemptResponse> getGameAttemptCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok().body(gamePointService.getGameAttemptCount(userDetails.getUsername()));
    }

    @PostMapping("/attempt_count")
    public ResponseEntity<Void> subtractAttemptCount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("isSubtract") boolean isSubtract
    ) {
        gamePointService.updateGameAttemptCount(userDetails.getUsername(), isSubtract);
        return ResponseEntity.ok().body(null);
    }

    @PatchMapping("/point")
    public ResponseEntity<Integer> updatePoint(
            @RequestParam("newPoint") String newPoint,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok().body(gamePointService.updatePoint(userDetails.getUsername(), Integer.parseInt(newPoint)));
    }

}
