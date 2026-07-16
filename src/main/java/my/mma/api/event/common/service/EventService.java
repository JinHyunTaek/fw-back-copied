package my.mma.api.event.common.service;

import lombok.RequiredArgsConstructor;
import my.mma.api.event.common.dto.EventCardsDto;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.repository.PromotionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final int RECENT_PROMOTION_DAYS = 10;
    private final PromotionRepository promotionRepository;

    @Cacheable("recentEvents")
    public EventCardsDto getRecentEvents(){
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(RECENT_PROMOTION_DAYS);
        List<Promotion> recentPromotions = promotionRepository.findRecent(today, from);
        List<EventCardsDto.EventCardDto> eventCards = recentPromotions.stream()
                .map(EventCardsDto.EventCardDto::fromPromotion)
                .toList();
        return new EventCardsDto(eventCards);
    }

}
