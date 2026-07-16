package my.mma.admin.web.promotion.service;

import my.mma.admin.web.promotion.dto.AdminGifticonSaveRequest;
import my.mma.admin.web.promotion.dto.AdminPromotionSaveRequest;
import my.mma.admin.web.promotion.dto.AdminPromotionUpdateRequest;
import my.mma.admin.web.promotion.dto.AdminPromotionUpdateRequest.AdminGifticonUpdateRequest;
import my.mma.admin.web.promotion.repository.AdminPromotionRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.event.promotion.entity.Gifticon;
import my.mma.api.event.promotion.entity.GifticonCategory;
import my.mma.api.event.promotion.entity.Promotion;
import my.mma.api.event.promotion.repository.GifticonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPromotionService - 기프티콘 유효기간 검증(당첨자 발표일 + 10일 이상)")
class AdminPromotionServiceTest {

    @Mock
    private AdminPromotionRepository adminPromotionRepository;
    @Mock
    private GifticonRepository gifticonRepository;
    @Mock
    private S3Client s3Client;
    @Mock
    private S3ImgService s3ImgService;

    @InjectMocks
    private AdminPromotionService adminPromotionService;

    // 당첨자 발표일 기준 유효기간 임계값 = 발표일 + 10일 (이날 포함 이후만 허용)
    private static final LocalDate ANNOUNCE_DATE = LocalDate.of(2026, 7, 1);
    private static final LocalDate EXPIRY_THRESHOLD = ANNOUNCE_DATE.plusDays(10); // 2026-07-11
    private static final long PROMOTION_ID = 1L;

    private MultipartFile image() {
        return new MockMultipartFile("image", "gifticon.png", "image/png", new byte[]{1, 2, 3});
    }

    private Promotion promotion(int maxWinnerCount, boolean drawn) {
        Promotion promotion = Promotion.builder()
                .id(PROMOTION_ID)
                .title("여름 프로모션")
                .benefit("스타벅스 아메리카노")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .announceDate(ANNOUNCE_DATE)
                .maxWinnerCount(maxWinnerCount)
                .notice("유의사항")
                .build();
        if (drawn) {
            promotion.markDrawn();
        }
        return promotion;
    }

    private AdminPromotionSaveRequest saveRequest(LocalDate expiryDate, MultipartFile image) {
        return new AdminPromotionSaveRequest(
                "여름 프로모션", "스타벅스 아메리카노",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), ANNOUNCE_DATE,
                1, "유의사항",
                List.of(new AdminGifticonSaveRequest("아메리카노", "COUPON-001", expiryDate, GifticonCategory.COFFEE, 1, image))
        );
    }

    @Nested
    @DisplayName("save() - 프로모션 + 기프티콘 신규 등록")
    class Save {

        @Test
        @DisplayName("유효기간이 발표일+10일(경계)이면 정상 저장된다")
        void saveSucceedsWhenExpiryIsExactlyThreshold() {
            // given
            AdminPromotionSaveRequest request = saveRequest(EXPIRY_THRESHOLD, image());
            when(adminPromotionRepository.save(any(Promotion.class))).thenReturn(promotion(1, false));

            // when
            adminPromotionService.save(request);

            // then - 프로모션 저장 + 기프티콘 1건 저장 + S3 업로드 1회 수행
            ArgumentCaptor<List<Gifticon>> captor = ArgumentCaptor.forClass(List.class);
            verify(adminPromotionRepository).save(any(Promotion.class));
            verify(gifticonRepository).saveAll(captor.capture());
            verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

            List<Gifticon> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.getFirst().getExpiryDate()).isEqualTo(EXPIRY_THRESHOLD);
            assertThat(saved.getFirst().getImageKey()).isNotBlank();
        }

        @Test
        @DisplayName("유효기간이 발표일+9일이면 INVALID_EXPIRY_DATE_400 예외가 발생한다")
        void saveFailsWhenExpiryIsOneDayBeforeThreshold() {
            // given
            AdminPromotionSaveRequest request = saveRequest(EXPIRY_THRESHOLD.minusDays(1), image());

            // when & then
            assertThatThrownBy(() -> adminPromotionService.save(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_EXPIRY_DATE_400.name());
        }

        @Test
        @DisplayName("검증 실패 시 S3 업로드/프로모션·기프티콘 저장이 일어나지 않는다 (고아 객체 방지)")
        void saveDoesNotTouchS3OrDbWhenExpiryInvalid() {
            // given
            AdminPromotionSaveRequest request = saveRequest(EXPIRY_THRESHOLD.minusDays(1), image());

            // when
            assertThatThrownBy(() -> adminPromotionService.save(request))
                    .isInstanceOf(CustomException.class);

            // then
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(adminPromotionRepository, never()).save(any());
            verify(gifticonRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("기프티콘 수가 최대 당첨자 수와 다르면 BAD_REQUEST_400 예외가 발생한다")
        void saveFailsWhenGifticonCountMismatch() {
            // given - maxWinnerCount=2 인데 기프티콘은 1건
            AdminPromotionSaveRequest request = new AdminPromotionSaveRequest(
                    "여름 프로모션", "스타벅스 아메리카노",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), ANNOUNCE_DATE,
                    2, "유의사항",
                    List.of(new AdminGifticonSaveRequest("아메리카노", "COUPON-001", EXPIRY_THRESHOLD, GifticonCategory.COFFEE, 1, image()))
            );

            // when & then
            assertThatThrownBy(() -> adminPromotionService.save(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BAD_REQUEST_400.name());
        }

        @Test
        @DisplayName("이미지가 없으면 BAD_REQUEST_400 예외가 발생한다")
        void saveFailsWhenImageMissing() {
            // given
            AdminPromotionSaveRequest request = saveRequest(EXPIRY_THRESHOLD, new MockMultipartFile("image", new byte[0]));

            // when & then
            assertThatThrownBy(() -> adminPromotionService.save(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BAD_REQUEST_400.name());
        }
    }

    @Nested
    @DisplayName("saveGifticon() - 기존 프로모션에 기프티콘 1건 추가")
    class SaveGifticon {

        @Test
        @DisplayName("유효기간이 발표일+10일 이후면 기프티콘이 저장되고 최대 당첨자 수가 1 증가한다")
        void saveGifticonSucceedsAndIncrementsMaxWinnerCount() {
            // given
            Promotion promotion = promotion(1, false);
            when(adminPromotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
            AdminGifticonSaveRequest request =
                    new AdminGifticonSaveRequest("아메리카노", "COUPON-002", EXPIRY_THRESHOLD.plusDays(5), GifticonCategory.COFFEE, 1, image());

            // when
            adminPromotionService.saveGifticon(PROMOTION_ID, request);

            // then
            assertThat(promotion.getMaxWinnerCount()).isEqualTo(2);
            verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(gifticonRepository).save(any(Gifticon.class));
        }

        @Test
        @DisplayName("유효기간이 발표일+9일이면 INVALID_EXPIRY_DATE_400 예외가 발생하고 부수효과가 없다")
        void saveGifticonFailsWhenExpiryInvalid() {
            // given
            Promotion promotion = promotion(1, false);
            when(adminPromotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
            AdminGifticonSaveRequest request =
                    new AdminGifticonSaveRequest("아메리카노", "COUPON-002", EXPIRY_THRESHOLD.minusDays(1), GifticonCategory.COFFEE, 1, image());

            // when & then
            assertThatThrownBy(() -> adminPromotionService.saveGifticon(PROMOTION_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_EXPIRY_DATE_400.name());

            assertThat(promotion.getMaxWinnerCount()).isEqualTo(1); // 변동 없음
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(gifticonRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 추첨 완료된 프로모션이면 유효기간 검증 이전에 PROMOTION_ALREADY_DRAWN_400 예외가 발생한다")
        void saveGifticonFailsWhenPromotionAlreadyDrawn() {
            // given - 추첨 완료 + 유효기간은 정상값
            Promotion promotion = promotion(1, true);
            when(adminPromotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
            AdminGifticonSaveRequest request =
                    new AdminGifticonSaveRequest("아메리카노", "COUPON-002", EXPIRY_THRESHOLD.plusDays(5), GifticonCategory.COFFEE, 1, image());

            // when & then
            assertThatThrownBy(() -> adminPromotionService.saveGifticon(PROMOTION_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.PROMOTION_ALREADY_DRAWN_400.name());

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(gifticonRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update() - 프로모션/기프티콘 수정")
    class Update {

        private AdminPromotionUpdateRequest updateRequest(Long gifticonId, LocalDate expiryDate) {
            return new AdminPromotionUpdateRequest(
                    "여름 프로모션(수정)", "스타벅스 라떼",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), ANNOUNCE_DATE,
                    1, "유의사항(수정)",
                    List.of(new AdminGifticonUpdateRequest(gifticonId, "라떼", "COUPON-003", expiryDate, GifticonCategory.COFFEE, 1, null))
            );
        }

        private Gifticon gifticonOf(Promotion promotion, long gifticonId) {
            return Gifticon.builder()
                    .id(gifticonId)
                    .name("아메리카노")
                    .couponNumber("COUPON-001")
                    .expiryDate(EXPIRY_THRESHOLD)
                    .imageKey("old-key")
                    .promotion(promotion)
                    .build();
        }

        @Test
        @DisplayName("유효기간이 발표일+9일이면 INVALID_EXPIRY_DATE_400 예외가 발생한다 (수정 경로에서도 규칙 적용)")
        void updateFailsWhenExpiryInvalid() {
            // given
            long gifticonId = 10L;
            Promotion promotion = promotion(1, false);
            when(adminPromotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
            when(gifticonRepository.findById(gifticonId)).thenReturn(Optional.of(gifticonOf(promotion, gifticonId)));

            // when & then
            assertThatThrownBy(() ->
                    adminPromotionService.update(PROMOTION_ID, updateRequest(gifticonId, EXPIRY_THRESHOLD.minusDays(1))))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_EXPIRY_DATE_400.name());
        }

        @Test
        @DisplayName("유효기간이 유효하면 기프티콘과 프로모션 정보가 갱신된다")
        void updateSucceedsAndAppliesChanges() {
            // given
            long gifticonId = 10L;
            Promotion promotion = promotion(1, false);
            Gifticon gifticon = gifticonOf(promotion, gifticonId);
            when(adminPromotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
            when(gifticonRepository.findById(gifticonId)).thenReturn(Optional.of(gifticon));

            LocalDate newExpiry = EXPIRY_THRESHOLD.plusDays(3);

            // when
            adminPromotionService.update(PROMOTION_ID, updateRequest(gifticonId, newExpiry));

            // then - 더티 체킹 대상 엔티티가 실제로 갱신됐는지 검증
            assertThat(promotion.getTitle()).isEqualTo("여름 프로모션(수정)");
            assertThat(promotion.getBenefit()).isEqualTo("스타벅스 라떼");
            assertThat(gifticon.getName()).isEqualTo("라떼");
            assertThat(gifticon.getCouponNumber()).isEqualTo("COUPON-003");
            assertThat(gifticon.getExpiryDate()).isEqualTo(newExpiry);
            // 이미지 미첨부 → 교체/업로드 없음
            assertThat(gifticon.getImageKey()).isEqualTo("old-key");
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }
    }
}
