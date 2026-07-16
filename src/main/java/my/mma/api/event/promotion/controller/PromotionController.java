package my.mma.api.event.promotion.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.event.promotion.dto.HomePromotionsDto;
import my.mma.api.event.promotion.dto.PromotionDetailResponse;
import my.mma.api.event.promotion.service.PromotionCacheService;
import my.mma.api.event.promotion.service.PromotionDetailService;
import my.mma.api.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionCacheService activePromotionCacheService;
    private final PromotionDetailService promotionDetailService;

    @GetMapping("")
    public ResponseEntity<HomePromotionsDto> active() {
        return ResponseEntity.ok()
                .body(activePromotionCacheService.getActivePromotions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDetailResponse> detail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ){
        return ResponseEntity.ok()
                .body(promotionDetailService.getDetail(userDetails.getUsername(), id));
    }

}
