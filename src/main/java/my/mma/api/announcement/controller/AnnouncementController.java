package my.mma.api.announcement.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.announcement.dto.AnnouncementContentDto;
import my.mma.api.announcement.dto.AnnouncementDto;
import my.mma.api.announcement.service.AnnouncementService;
import my.mma.api.global.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/announcements")
    public ResponseEntity<PageResponse<AnnouncementDto>> getAnnouncements(
            @PageableDefault(sort = {"pinned", "createdDateTime"}, direction = DESC) Pageable pageable
    ){
        return ResponseEntity.ok().body(announcementService.getAnnouncements(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementContentDto> getAnnouncement(
            @PathVariable("id") Long id
    ){
        return ResponseEntity.ok().body(announcementService.getAnnouncementContent(id));
    }

}
