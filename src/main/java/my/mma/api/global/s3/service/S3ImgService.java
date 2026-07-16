package my.mma.api.global.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3ImgService {

    private static final String BODY_OBJECT_KEY_PREFIX = "body/";
    private static final String HEADSHOT_OBJECT_KEY_PREFIX = "headshot/";

    // 선수 이미지 존재 여부 캐시. 이미지는 릴리즈 내내 고정(간헐적 수동 업로드만)이므로
    // 존재 여부를 하루 캐싱해 S3 headObject 호출을 줄인다. 수동 업로드분은 TTL 만료 후 반영.
    private static final String IMG_EXISTS_KEY_PREFIX = "fighter:img:exists:";
    private static final Duration IMG_EXISTS_TTL = Duration.ofDays(1);

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final StringRedisTemplate fighterImgExistsRedisTemplate;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String ufcFighterImgBucketName;

    @Value("${spring.cloud.aws.s3.user-img-bucket}")
    private String userBucketName;

    @Value("${spring.cloud.aws.s3.gifticon-img-bucket}")
    private String gifticonImgBucketName;

    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    public String generateFighterHeadshotUrlOrNull(String name) {
        String objectKey = HEADSHOT_OBJECT_KEY_PREFIX + fighterKeyName(name);
        return objectExistsCached(objectKey) ? toCloudFrontUrl(objectKey) : null;
    }

    public String generateFighterBodyUrlOrNull(String name) {
        String objectKey = BODY_OBJECT_KEY_PREFIX + fighterKeyName(name);
        return objectExistsCached(objectKey) ? toCloudFrontUrl(objectKey) : null;
    }

    // 파이썬 업로드 스크립트의 파일명 규칙과 반드시 일치: 공백→'_', 전부 소문자, 확장자 없음.
    // Locale.ROOT 고정 — 서버 로케일이 tr 등이면 'I'→'ı'로 깨져 'Islam' 같은 이름의 키가 어긋남.
    private String fighterKeyName(String name) {
        return name.replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    public String generateUserImgUrl(Long userId) {
        return generateImgUrlFromObjectKey(userImgKey(userId));
    }

    public String generateUserImgUrlOrNull(Long userId) {
        String objectKey = userImgKey(userId);
        objectKey = objectExists(objectKey) ? objectKey : null;
        if (objectKey == null)
            return null;
        return generateImgUrlFromObjectKey(objectKey);
    }

    // 기프티콘 이미지는 쿠폰 정보가 담긴 민감 자료 → 공개 CloudFront가 아닌 presigned URL로 제공
    public String generateGifticonImgUrl(String imageKey) {
        if (imageKey == null) {
            return null;
        }
        GetObjectRequest aclRequest = GetObjectRequest.builder()
                .bucket(gifticonImgBucketName)
                .key(imageKey)
                .build();
        return getPresignedUrl(aclRequest);
    }

    public byte[] getGifticonImageBytes(String imageKey) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(gifticonImgBucketName).key(imageKey).build()).asByteArray();
    }

    // 기프티콘 이미지 삭제 (프로모션 삭제 시 S3 고아 객체 정리)
    public void deleteGifticonImg(String imageKey) {
        if (imageKey == null) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(gifticonImgBucketName)
                    .key(imageKey)
                    .build());
        } catch (Exception e) {
            // DB 삭제는 이미 끝났으므로 S3 삭제 실패는 흐름을 막지 않고 로그만 남김 (고아 객체로 남을 뿐)
            log.warn("기프티콘 이미지 삭제 실패. bucket={}, key={}", gifticonImgBucketName, imageKey, e);
        }
    }

    private String generateImgUrlFromObjectKey(String objectKey) {
        GetObjectRequest aclRequest = GetObjectRequest.builder()
                .bucket(getBucketFromKey(objectKey))
                .key(objectKey)
                .build();
        return getPresignedUrl(aclRequest);
    }

    // 선수 이미지 존재 여부를 Redis에 캐싱(TTL 1일). 부재("false")도 캐싱해 없는 선수 반복 조회를 막는다.
    // 이미지 갱신은 더 이상 API가 하지 않으므로(수동 업로드만) 캐시 무효화 로직은 두지 않는다.
    private boolean objectExistsCached(String objectKey) {
        String redisKey = IMG_EXISTS_KEY_PREFIX + objectKey;
        String cached = fighterImgExistsRedisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            return "true".equals(cached);
        }
        boolean existsInS3 = objectExists(objectKey);
        fighterImgExistsRedisTemplate.opsForValue().set(redisKey, Boolean.toString(existsInS3), IMG_EXISTS_TTL);
        return existsInS3;
    }

    private boolean objectExists(String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(getBucketFromKey(objectKey))
                    .key(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getBucketFromKey(String objectKey) {
        return objectKey.contains(HEADSHOT_OBJECT_KEY_PREFIX) ||
                objectKey.contains(BODY_OBJECT_KEY_PREFIX)
                ? ufcFighterImgBucketName : userBucketName;
    }

    private String getPresignedUrl(GetObjectRequest aclRequest) {
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(builder -> builder
                .getObjectRequest(aclRequest)
                .signatureDuration(Duration.ofHours(2)));
        return presignedRequest.url().toString();
    }

    private String toCloudFrontUrl(String objectKey) {
        return cloudfrontDomain + "/" + objectKey;
    }

    public String userImgKey(Long userId) {
        return "users/" + userId;
    }

}