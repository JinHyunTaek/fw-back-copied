package my.mma.admin.web.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.promotion.dto.*;
import my.mma.admin.web.promotion.repository.AdminPromotionRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.repository.GifticonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class AdminPromotionService {

    // 당첨자가 수령 후 사용할 최소한의 기간 보장
    private static final int MIN_EXPIRY_DAYS_AFTER_ANNOUNCE = 10;

    @Value("${spring.cloud.aws.s3.gifticon-img-bucket}")
    private String gifticonImgBucket;

    private final AdminPromotionRepository adminPromotionRepository;
    private final GifticonRepository gifticonRepository;
    private final S3Client s3Client;
    private final S3ImgService s3ImgService;

    public Page<AdminPromotionResponse> get(Pageable pageable) {
        return adminPromotionRepository.getPromotions(pageable);
    }

    public AdminPromotionDetailResponse detail(Long id) {
        Promotion promotion = adminPromotionRepository.findById(id).orElseThrow(
                () -> new CustomException(ErrorCode.BAD_REQUEST_400)
        );
        List<Gifticon> gifticons = gifticonRepository.findByPromotionIdOrderByDisplayOrderAsc(promotion.getId());
        return AdminPromotionDetailResponse.of(promotion, gifticons, s3ImgService::generateGifticonImgUrl);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "activePromotions", allEntries = true),
            @CacheEvict(value = "recentEvents", allEntries = true),
            @CacheEvict(value = "promotionDetail", key = "#id")
    })
    public void delete(Long id) {
        Promotion promotion = adminPromotionRepository.findById(id).orElseThrow(
                () -> new CustomException(ErrorCode.BAD_REQUEST_400)
        );
        ensureNotDrawn(promotion);
        if (promotion.getAnnounceDate().isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.BAD_REQUEST_400);
        }
        List<Gifticon> gifticons = gifticonRepository.findByPromotionIdOrderByDisplayOrderAsc(id);
        gifticonRepository.deleteByPromotionId(id);
        adminPromotionRepository.deleteById(id);
        gifticons.forEach(g -> s3ImgService.deleteGifticonImg(g.getImageKey())); // S3 정리
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "activePromotions", allEntries = true),
            @CacheEvict(value = "recentEvents", allEntries = true),
            @CacheEvict(value = "promotionDetail", key = "#id")
    })
    public void update(Long id, AdminPromotionUpdateRequest request) {
        Promotion promotion = adminPromotionRepository.findById(id).orElseThrow(
                () -> new CustomException(ErrorCode.BAD_REQUEST_400)
        );
        ensureNotDrawn(promotion);
        if (request.gifticons() == null || request.gifticons().size() != request.maxWinnerCount())
            throw new CustomException(ErrorCode.BAD_REQUEST_400);

        promotion.update(request.title(), request.benefit(), request.startDate(),
                request.endDate(), request.announceDate(),
                request.maxWinnerCount(), request.notice()
        );

        List<String> oldImageKeysToDelete = new ArrayList<>();
        request.gifticons().forEach(req -> {
            Gifticon gifticon = gifticonRepository.findById(req.id()).orElseThrow(
                    () -> new CustomException(ErrorCode.BAD_REQUEST_400)
            );
            if (!gifticon.getPromotion().getId().equals(id)) {
                throw new CustomException(ErrorCode.BAD_REQUEST_400);
            }
            validateExpiryDate(req.expiryDate(), request.announceDate());
            gifticon.update(req.name(), req.couponNumber(), req.expiryDate(), req.category(), req.displayOrder());

            // 새 이미지가 첨부된 경우에만 교체
            if (req.image() != null && !req.image().isEmpty()) {
                String oldKey = gifticon.getImageKey();
                gifticon.changeImageKey(uploadGifticonImage(req.image()));
                if (oldKey != null) {
                    oldImageKeysToDelete.add(oldKey);
                }
            }
        });

        // 기존 이미지 삭제는 커밋 성공 후에만 (롤백 시 깨진 참조 방지)
        if (!oldImageKeysToDelete.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    oldImageKeysToDelete.forEach(s3ImgService::deleteGifticonImg);
                }
            });
        }
    }

    @Transactional
    @CacheEvict(value = {"activePromotions","recentEvents"}, allEntries = true)
    public void save(AdminPromotionSaveRequest request) {
        if (request.gifticonSaveRequests() == null || request.gifticonSaveRequests().size() != request.maxWinnerCount())
            throw new CustomException(ErrorCode.BAD_REQUEST_400);

        // S3 업로드 전에 모든 기프티콘을 먼저 검증한다.
        // (일부만 업로드된 뒤 검증 실패로 트랜잭션이 롤백되면 S3에 고아 객체가 남기 때문)
        request.gifticonSaveRequests().forEach(gifReq -> {
            if (gifReq.image() == null || gifReq.image().isEmpty()) {
                throw new CustomException(ErrorCode.BAD_REQUEST_400);
            }
            validateExpiryDate(gifReq.expiryDate(), request.announceDate());
        });

        Promotion promotion = adminPromotionRepository.save(request.toEntity());

        List<Gifticon> gifticons = request.gifticonSaveRequests().stream()
                .map(gifReq -> gifReq.toEntity(promotion, uploadGifticonImage(gifReq.image())))
                .toList();
        gifticonRepository.saveAll(gifticons);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "activePromotions", allEntries = true),
            @CacheEvict(value = "promotionDetail", key = "#promotionId")
    })
    public void saveGifticon(Long promotionId, AdminGifticonSaveRequest request) {
        Promotion promotion = adminPromotionRepository.findById(promotionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        ensureNotDrawn(promotion);
        if (request.image() == null || request.image().isEmpty())
            throw new CustomException(ErrorCode.BAD_REQUEST_400);
        validateExpiryDate(request.expiryDate(), promotion.getAnnounceDate());

        String imageKey = uploadGifticonImage(request.image());
        promotion.updateMaxWinnerCount(promotion.getMaxWinnerCount() + 1);
        gifticonRepository.save(request.toEntity(promotion, imageKey));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "activePromotions", allEntries = true),
            @CacheEvict(value = "promotionDetail", key = "#promotionId")
    })
    public void deleteGifticon(Long promotionId, Long gifticonId) {
        Gifticon gifticon = gifticonRepository.findById(gifticonId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        Promotion promotion = gifticon.getPromotion();
        if (!promotion.getId().equals(promotionId)) {
            throw new CustomException(ErrorCode.BAD_REQUEST_400);
        }
        ensureNotDrawn(promotion);

        // 기프티콘 1개 삭제 → 최대 당첨자 수도 1 감소 (saveGifticon의 +1과 대칭, 불변식 유지)
        promotion.updateMaxWinnerCount(promotion.getMaxWinnerCount() - 1);

        String imageKey = gifticon.getImageKey();
        gifticonRepository.delete(gifticon);

        // S3 이미지는 커밋 성공 후에만 삭제 (롤백 시 깨진 참조 방지)
        if (imageKey != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    s3ImgService.deleteGifticonImg(imageKey);
                }
            });
        }
    }

    // 추첨 완료(drawnAt != null)된 프로모션은 수정/삭제/기프티콘 추가·삭제 모두 불가
    private void ensureNotDrawn(Promotion promotion) {
        if (promotion.isDrawn())
            throw new CustomException(ErrorCode.PROMOTION_ALREADY_DRAWN_400);
    }

    // (경계 포함: 발표일로부터 정확히 10일째 되는 날짜는 허용)
    private void validateExpiryDate(LocalDate expiryDate, LocalDate announceDate) {
        if (expiryDate.isBefore(announceDate.plusDays(MIN_EXPIRY_DAYS_AFTER_ANNOUNCE)))
            throw new CustomException(ErrorCode.INVALID_EXPIRY_DATE_400);
    }

    private String uploadGifticonImage(MultipartFile image) {
        String key = UUID.randomUUID() + resolveExtension(image);
        try (InputStream inputStream = image.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(gifticonImgBucket)
                    .key(key)
                    .contentType(image.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, image.getSize()));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }
        return key;
    }

    private String resolveExtension(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null) return "";
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }

}
