package my.mma.api.fightevent.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.fightevent.dto.FightEventDto;
import my.mma.api.fightevent.dto.FighterFightEventCardDetailDto;
import my.mma.api.fightevent.service.FightEventService;
import my.mma.api.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/event")
public class FightEventController {

    private final FightEventService eventService;

    @GetMapping("/days")
    public ResponseEntity<List<Integer>> getDaysFromYearMonth(
            @RequestParam(value = "year") int year,
            @RequestParam(value = "month") int month
    ){
        return ResponseEntity.ok().body(eventService.getEventDaysFromYearMonth(year, month));
    }

    // get eventDate for parameter
    @GetMapping("/detail")
    public ResponseEntity<List<FightEventDto>> detail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "date",required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok().body(eventService.getSchedule(date,userDetails.getUsername()));
    }

    @GetMapping("/events")
    public ResponseEntity<Page<FightEventDto.FighterFightEventDto>> search(
            @RequestParam(value = "name",defaultValue = "") String name,
            @PageableDefault(sort = "eventDate", direction = DESC) Pageable pageable
    ) {
        return ResponseEntity.ok().body(eventService.search(name,pageable));
    }

    @GetMapping("/card/detail")
    public ResponseEntity<FighterFightEventCardDetailDto> cardDetail(
            @RequestParam(value = "cardId") String cardId
    ){
        return ResponseEntity.ok().body(eventService.cardDetail(Long.parseLong(cardId)));
    }

}
