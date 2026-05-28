package my.mma.admin.web.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.dto.user.AdminUserPunishmentRequest;
import my.mma.admin.web.dto.user.AdminUserDetailResponse;
import my.mma.admin.web.dto.user.AdminUserResponse;
import my.mma.admin.web.dto.user.AdminUserBetPointUpdateRequest;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.user.entity.User;
import my.mma.api.user.entity.PunishedUser;
import my.mma.api.user.repository.PunishedUserRepository;
import my.mma.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final PunishedUserRepository punishedUserRepository;
    private final S3ImgService s3Service;

    public Page<AdminUserResponse> users(Pageable pageable, String nickname, Boolean betUnsettled){
        // isBlank는 화이트스페이스 필터링
        if(nickname != null && !nickname.isBlank())
            return userRepository.findUsersByNicknameContainsIgnoreCase(pageable, nickname)
                    .map(user -> AdminUserResponse.toDto(user, s3Service.generateUserImgUrlOrNull((user.getId()))));
        if(Boolean.TRUE.equals(betUnsettled))
            return userRepository.findUsersWithUnsettledBets(pageable)
                    .map(user -> AdminUserResponse.toDto(user, s3Service.generateUserImgUrlOrNull(user.getId())));
        return userRepository.findAll(pageable)
                .map(user -> AdminUserResponse.toDto(user, s3Service.generateUserImgUrlOrNull(user.getId())));
    }

    public AdminUserDetailResponse detail(Long userId){
        User user = getUser(userId);
        Optional<PunishedUser> punishedUser = punishedUserRepository.findById(user.getId());
        return AdminUserDetailResponse.toDto(user, punishedUser.isPresent(), s3Service.generateUserImgUrlOrNull(userId));
    }

    @Transactional
    public void updatePoint(Long id, AdminUserBetPointUpdateRequest request){
        User user = getUser(id);
        user.updatePoint(request.point());
        user.updateEarnedBetSucceedPoint(request.earnedBetSucceedPoint());
    }

    public void updatePunishmentState(Long id, AdminUserPunishmentRequest request){
        if(request.punish()){
            punishedUserRepository.save(PunishedUser.builder()
                    .userId(id)
                    .expiration(Duration.ofDays(7).toSeconds())
                    .reportCategory(request.reportCategory())
                    .build());
        }else{
            punishedUserRepository.deleteById(id);
        }
    }

    private User getUser(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
    }

}
