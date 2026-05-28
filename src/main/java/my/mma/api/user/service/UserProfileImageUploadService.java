package my.mma.api.user.service;

import lombok.RequiredArgsConstructor;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserProfileImageUploadService {

    @Value("${spring.cloud.aws.s3.user-img-bucket}")
    private String userImgBucket;

    private final UserRepository userRepository;
    private final S3Client s3Client;
    private final S3ImgService s3Service;

    public String uploadProfileImage(String email, MultipartFile file){
        User user = getUser(email);
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new CustomException(ErrorCode.INVALID_FILE_400);

        if (!Objects.requireNonNull(file.getContentType()).startsWith("image/"))
            throw new CustomException(ErrorCode.INVALID_FILE_400);
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(userImgBucket)
                    .key(s3Service.userImgKey(user.getId()))
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }
        return s3Service.generateUserImgUrl(user.getId());
    }

    public void deleteProfileImage(String email){
        User user = getUser(email);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(userImgBucket)
                .key(s3Service.userImgKey(user.getId()))
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_USER_FOUND_400));
    }

}
