package my.mma.api.fighter.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fighter.dto.FighterRatingDto;
import my.mma.api.fighter.dto.FighterRatingRequest;
import my.mma.api.fightevent.dto.FightEventDto.FighterFightEventDto;
import my.mma.api.fighter.dto.FighterDetailDto;
import my.mma.api.fighter.service.FighterService;
import my.mma.api.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@Slf4j
@RequestMapping("/fighter")
@RequiredArgsConstructor
public class FighterController {

    private final FighterService fighterService;

    @GetMapping("/{fighterId}")
    public ResponseEntity<FighterDetailDto> detail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long fighterId) {
        return ResponseEntity.ok().body(fighterService.detail(userDetails.getUsername(), fighterId));
    }

    @GetMapping("/{fighterId}/fights")
    public ResponseEntity<List<FighterFightEventDto>> fightsByYear(
            @PathVariable Long fighterId,
            @RequestParam(value = "year") int year
    ) {
        return ResponseEntity.ok().body(fighterService.getFighterFightEventsByYear(fighterId, year));
    }

    @GetMapping("/fighters")
    public ResponseEntity<Page<FighterDto>> search(
            @RequestParam(value = "name", defaultValue = "") String name,
            @PageableDefault(sort = "name", direction = ASC) Pageable pageable
    ) {
        return ResponseEntity.ok().body(fighterService.search(name, pageable));
    }

    @GetMapping("/rating")
    public ResponseEntity<Page<FighterRatingDto>> avgRatingRank(
            @PageableDefault(sort = "avgRating", direction = DESC) Pageable pageable
    ){
        return ResponseEntity.ok().body(fighterService.getAvgRatingRank(pageable));
    }

    @PostMapping("/rating")
    public ResponseEntity<Void> updateRating(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Validated @RequestBody FighterRatingRequest request
    ) {
        fighterService.rate(userDetails.getUsername(), request);
        return ResponseEntity.ok().body(null);
    }

//    @GetMapping("/headshot")
//    public ResponseEntity<Map<String, String>> headshotUrl(
//            @RequestParam("name") String name
//    ) {
//        String preSignedUrl = s3Service.generateFighterHeadshotUrl(name);
//        Map<String, String> map = new HashMap<>();
//        map.put("url", preSignedUrl);
//        return ResponseEntity.ok().body(map);
//    }
//
//    @GetMapping("/body")
//    public ResponseEntity<Map<String, String>> bodyUrl(
//            @RequestParam("name") String name
//    ) {
//        String preSignedUrl = s3Service.generateFighterBodyUrl(name);
//        Map<String, String> map = new HashMap<>();
//        map.put("url", preSignedUrl);
//        return ResponseEntity.ok().body(map);
//    }

}