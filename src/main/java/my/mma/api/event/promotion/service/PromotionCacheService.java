package my.mma.api.event.promotion.service;

import lombok.RequiredArgsConstructor;
import my.mma.api.event.promotion.dto.HomePromotionsDto;
import my.mma.api.event.promotion.dto.HomePromotionsDto.HomePromotionDto;
import my.mma.api.event.promotion.dto.PromotionDetailDto.PromotionWinnerGifticonDto;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.event.promotion.dto.PromotionDetailDto;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.repository.GifticonRepository;
import my.mma.api.event.promotion.repository.PromotionRepository;
import my.mma.api.event.promotion.repository.PromotionWinnerRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class PromotionCacheService {

    private final PromotionRepository promotionRepository;
    private final PromotionWinnerRepository promotionWinnerRepository;
    private final GifticonRepository gifticonRepository;

    @Cacheable(value = "activePromotions")
    public HomePromotionsDto getActivePromotions(){
        List<Promotion> activePromotions = promotionRepository.findActive(LocalDate.now());
        List<HomePromotionDto> promotions = activePromotions.stream()
                .map(HomePromotionDto::toDto).toList();
        return new HomePromotionsDto(promotions);
    }

    @Cacheable(value = "promotionDetail", key = "#id")
    public PromotionDetailDto getPromotionDetail(Long id){
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        List<PromotionWinnerGifticonDto> winnerGifticons = new ArrayList<>();
        if(promotion.isDrawn()){
            winnerGifticons = promotionWinnerRepository.findWinnerGifticonInfoByPromotionId(promotion.getId())
                    .stream().map(PromotionWinnerGifticonDto::of).toList();
        }
        List<Gifticon> gifticons = gifticonRepository.findByPromotionIdOrderByDisplayOrderAsc(promotion.getId());
        return PromotionDetailDto.toDto(promotion, winnerGifticons, gifticons);
    }

}
