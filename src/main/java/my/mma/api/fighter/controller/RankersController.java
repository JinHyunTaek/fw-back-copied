package my.mma.api.fighter.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fighter.dto.RankersPerCategory;
import my.mma.api.fighter.service.RankerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/rankers")
@RequiredArgsConstructor
public class RankersController {

    private final RankerService rankerService;

    @GetMapping("")
    public ResponseEntity<RankersPerCategory> rankersPerCategory(
            @RequestParam("category") String category
    ) {
        return ResponseEntity.ok().body(rankerService.rankers(RankersPerCategory.RankingCategory.valueOf(category)));
    }

}
