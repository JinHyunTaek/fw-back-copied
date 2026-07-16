package my.mma.admin.web.stream.controller;

import lombok.RequiredArgsConstructor;
import my.mma.admin.web.stream.service.AdminStreamFightEventService;
import my.mma.api.fightevent.entity.FightEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/htj-admin/stream")
public class AdminStreamFightEventController {

    private final AdminStreamFightEventService streamFightEventService;

    @GetMapping("/event")
    public String redirectToCurrentEvent() {
        List<FightEvent> upcomingEvents = streamFightEventService.getUpcomingEvents();
        if (upcomingEvents.isEmpty()) {
            return "redirect:/htj-admin/update-buttons";
        }
        return "redirect:/htj-admin/stream/event/" + upcomingEvents.getFirst().getId();
    }

    @GetMapping("/event/{id}")
    public String eventDetail(@PathVariable Long id, Model model) {
        FightEvent event = streamFightEventService.getEventWithFfes(id);
        List<FightEvent> upcomingEvents = streamFightEventService.getUpcomingEvents();
        model.addAttribute("event", event);
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("activeMenu", "stream-event");
        return "admin/stream/stream_fight_event";
    }
}