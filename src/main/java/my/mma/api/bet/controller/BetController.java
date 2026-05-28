package my.mma.api.bet.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.bet.dto.BetDeleteResponse;
import my.mma.api.bet.dto.BetResponse;
import my.mma.api.global.logaop.Loggable;
import my.mma.api.security.CustomUserDetails;
import my.mma.api.bet.service.BetService;
import my.mma.api.bet.dto.SingleBetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.http.HttpStatusCode;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bet")
public class BetController {

    private final BetService betService;

    // 기존 user의 betList에 singleBet을 추가하는 작업이므로, PatchMapping
    @Loggable
    @PatchMapping("")
    public ResponseEntity<Integer> bet(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @Validated @RequestBody SingleBetRequest betRequest) {
        return ResponseEntity.status(HttpStatusCode.CREATED).body(
                betService.predict(userDetails.getUsername(), betRequest)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<BetResponse> weeklyBetHistory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @RequestParam(value = "eventId") String eventId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                betService.predictionHistory(userDetails.getUsername(), Long.parseLong(eventId))
        );
    }

    @Loggable
    @DeleteMapping("")
    public ResponseEntity<BetDeleteResponse> deleteBet(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @RequestParam(value = "betId") String betId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                betService.deletePrediction(userDetails.getUsername(), Long.parseLong(betId))
        );
    }

}
