package my.mma.api.announcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.announcement.dto.AnnouncementContentDto;
import my.mma.admin.web.dto.announcement.AnnouncementDetailDto;
import my.mma.api.announcement.dto.AnnouncementDto;
import my.mma.admin.web.dto.announcement.AnnouncementRequest;
import my.mma.api.announcement.entity.Announcement;
import my.mma.api.announcement.repository.AnnounceRepository;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.global.dto.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnounceRepository announceRepository;

    @Cacheable(
            value = "announcements",
            key = "#p0.pageNumber + '_' + #p0.pageSize"
    )
    public PageResponse<AnnouncementDto> getAnnouncements(Pageable pageable) {
        Page<Announcement> announcements = announceRepository.findAll(pageable);
        Page<AnnouncementDto> announcementDtos = announcements.map(
                announcement -> AnnouncementDto.builder()
                        .id(announcement.getId())
                        .title(announcement.getTitle())
                        .pinned(announcement.isPinned())
                        .createdDate(announcement.getCreatedDateTime().toLocalDate())
                        .build());
        return PageResponse.from(announcementDtos);
    }

    // for user
    @Cacheable(value = "announcementContent", key = "#p0")
    public AnnouncementContentDto getAnnouncementContent(Long id) {
        Announcement announcement = getAnnouncement(id);
        return new AnnouncementContentDto(announcement.getContent());
    }

    // for admin
    public AnnouncementDetailDto detail(Long id) {
        Announcement announcement = getAnnouncement(id);
        return AnnouncementDetailDto.toDto(announcement);
    }

    @CacheEvict(value = "announcements", allEntries = true)
    @Transactional
    public Long save(AnnouncementRequest request) {
        return announceRepository.save(Announcement.builder()
                .title(request.title())
                .content(request.content())
                .pinned(request.pinned())
                .build()).getId();
    }

    @Caching(evict = {
            @CacheEvict(value = "announcements", allEntries = true),
            @CacheEvict(value = "announcementContent", key = "#p0")
    })
    @Transactional
    public void updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = getAnnouncement(id);
        announcement.updateAnnouncement(request.title(), request.content(), request.pinned());
    }

    @Caching(evict = {
            @CacheEvict(value = "announcements", allEntries = true),
            @CacheEvict(value = "announcementContent", key = "#p0")
    })
    @Transactional
    public void delete(Long id) {
        announceRepository.delete(getAnnouncement(id));
    }

    private Announcement getAnnouncement(Long id) {
        return announceRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
    }

}
