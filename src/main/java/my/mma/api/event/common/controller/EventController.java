package my.mma.api.event.common.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.event.common.dto.EventCardsDto;
import my.mma.api.event.common.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("")
    public ResponseEntity<EventCardsDto> getRecentEvents(){
        return ResponseEntity.ok().body(eventService.getRecentEvents());
    }

}
