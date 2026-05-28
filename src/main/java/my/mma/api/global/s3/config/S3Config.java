package my.mma.api.global.s3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${spring.cloud.aws.s3.region}")
    private String region;

    @Value("${spring.cloud.aws.s3.presign.access-key}")
    private String presignAccessKey;

    @Value("${spring.cloud.aws.s3.presign.secret-key}")
    private String presignSecretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(presignAccessKey, presignSecretKey)))
                .build();
    }

    @Bean
    public CloudFrontClient cloudFrontClient() {
        return CloudFrontClient.builder()
                .region(Region.AWS_GLOBAL)  // CloudFront는 글로벌 서비스
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

}
