package my.mma.api.global.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class S3ImgService {

    private static final String BODY_OBJECT_KEY_PREFIX = "body/";
    private static final String HEADSHOT_OBJECT_KEY_PREFIX = "headshot/";
    private static final String COMMON_SUFFIX = ".png";

//    private static final String IMG_EXISTS_KEY_PREFIX = "fighter:img:exists:";
//    private static final Duration IMG_EXISTS_TTL = Duration.ofDays(1);

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
//    private final CloudFrontClient cloudFrontClient;
//    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String ufcFighterImgBucketName;

    @Value("${spring.cloud.aws.s3.user-img-bucket}")
    private String userBucketName;

    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

//    @Value("${spring.cloud.aws.cloudfront.distribution-id}")
//    private String distributionId;

    public String generateFighterHeadshotUrl(String name) {
        return toCloudFrontUrl(HEADSHOT_OBJECT_KEY_PREFIX + name.replace(' ', '-') + COMMON_SUFFIX);
    }

    public String generateFighterBodyUrl(String name) {
        return toCloudFrontUrl(BODY_OBJECT_KEY_PREFIX + name.replace(' ', '-') + COMMON_SUFFIX);
    }

//    public String generateFighterHeadshotUrlOrNull(String name) {
//        String objectKey = HEADSHOT_OBJECT_KEY_PREFIX + name.replace(' ', '-') + COMMON_SUFFIX;
//        return objectExistsCached(objectKey) ? toCloudFrontUrl(objectKey) : null;
//    }
//
//    public String generateFighterBodyUrlOrNull(String name) {
//        String objectKey = BODY_OBJECT_KEY_PREFIX + name.replace(' ', '-') + COMMON_SUFFIX;
//        return objectExistsCached(objectKey) ? toCloudFrontUrl(objectKey) : null;
//    }

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

    private String generateImgUrlFromObjectKey(String objectKey) {
        GetObjectRequest aclRequest = GetObjectRequest.builder()
                .bucket(getBucketFromKey(objectKey))
                .key(objectKey)
                .build();
        return getPresignedUrl(aclRequest);
    }

    // evict both body & headshot (also redis & cloudfront)
//    public void evictFighterImgCache(String name) {
//        String nameWithExtension = name.replace(' ', '-') + COMMON_SUFFIX;
//        String headshotKey = IMG_EXISTS_KEY_PREFIX + HEADSHOT_OBJECT_KEY_PREFIX + nameWithExtension;
//        String bodyKey = IMG_EXISTS_KEY_PREFIX + BODY_OBJECT_KEY_PREFIX + nameWithExtension;
//        redisTemplate.delete(List.of(headshotKey, bodyKey));
//
//        cloudFrontClient.createInvalidation(r -> r
//                .distributionId(distributionId)
//                .invalidationBatch(b -> b
//                        .paths(p -> p
//                                .items(
//                                        "/" + HEADSHOT_OBJECT_KEY_PREFIX + nameWithExtension,
//                                        "/" + BODY_OBJECT_KEY_PREFIX + nameWithExtension
//                                )
//                                .quantity(2))
//                        .callerReference(String.valueOf(System.currentTimeMillis()))
//                )
//        );
//    }
//
//    private boolean objectExistsCached(String objectKey) {
//        String redisKey = IMG_EXISTS_KEY_PREFIX + objectKey;
//        String cached = redisTemplate.opsForValue().get(redisKey);
//        if (cached != null) {
//            return "true".equals(cached);
//        }
//        // s3에서의 존재여부를 cloudfront에서의 존재여부로 판단 (게임의 이미지도 cold start 제외하면 cloudfront 이미지를 사용하게 됨)
//        boolean existsInS3 = objectExists(objectKey);
//        redisTemplate.opsForValue().set(redisKey, existsInS3 ? "true" : "false", IMG_EXISTS_TTL);
//        return existsInS3;
//    }

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
        return "users/" + userId + COMMON_SUFFIX;
    }

}