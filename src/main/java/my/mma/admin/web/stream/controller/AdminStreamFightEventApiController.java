package my.mma.admin.web.stream.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import my.mma.admin.web.stream.service.AdminStreamFightEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/htj-admin/stream/event")
public class AdminStreamFightEventApiController {

    private final AdminStreamFightEventService service;

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEventInfo(
            @PathVariable Long id,
            @Validated @RequestBody EventInfoUpdateRequest request) {
        service.updateEventInfo(id,
                request.name(), request.displayDate(),
                request.earlyCardDate(), request.earlyCardTime(),
                request.prelimCardDate(), request.prelimCardTime(),
                request.mainCardDate(), request.mainCardTime(),
                request.earlyCardCnt(), request.prelimCardCnt(), request.mainCardCnt());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/ffe")
    public ResponseEntity<Void> addFfe(
            @PathVariable Long id,
            @Validated @RequestBody FfeAddRequest request) {
        service.addFfe(id, request.winnerId(), request.loserId(), request.fightWeight(), request.title(), request.cardOrder());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/ffe/{ffeId}")
    public ResponseEntity<Void> cancelFfe(@PathVariable Long ffeId) {
        service.cancelFfe(ffeId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/ffe/{ffeId}/reactivate")
    public ResponseEntity<Void> reactivateFfe(@PathVariable Long ffeId) {
        service.reactivateFfe(ffeId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/ffe/{ffeId}")
    public ResponseEntity<Void> updateFfe(
            @PathVariable Long ffeId,
            @RequestBody FfeUpdateRequest request) {
        service.updateFfe(ffeId, request.fightWeight(), request.title(), request.cardOrder());
        return ResponseEntity.ok().build();
    }

    public record EventInfoUpdateRequest(
            @NotBlank String name,
            @NotNull LocalDate displayDate,
            LocalDate earlyCardDate, LocalTime earlyCardTime,
            LocalDate prelimCardDate, LocalTime prelimCardTime,
            @NotNull LocalDate mainCardDate, @NotNull LocalTime mainCardTime,
            Integer earlyCardCnt, Integer prelimCardCnt, Integer mainCardCnt
    ) {}

    public record FfeAddRequest(@NotNull Long winnerId, @NotNull Long loserId, @NotNull String fightWeight, boolean title, @NotNull Integer cardOrder) {}

    public record FfeUpdateRequest(@NotNull String fightWeight, boolean title, @NotNull Integer cardOrder) {}
}