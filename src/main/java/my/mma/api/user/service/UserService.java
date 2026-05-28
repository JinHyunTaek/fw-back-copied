package my.mma.api.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.entity.UserPreferences;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.security.entity.PasswordResetToken;
import my.mma.api.security.repository.PasswordResetTokenRepository;
import my.mma.api.user.dto.*;
import my.mma.api.user.dto.RankedUserDto;
import org.springframework.data.domain.PageRequest;
import my.mma.api.user.entity.PunishedUser;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.PunishedUserRepository;
import my.mma.api.user.repository.UserRepository;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.user.entity.WithdrawalReason;
import my.mma.api.user.entity.WithdrawnUserEmail;
import my.mma.api.alert.repository.UserPreferencesRepository;
import my.mma.api.user.repository.WithdrawalReasonRepository;
import my.mma.api.user.repository.WithdrawnEmailRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static my.mma.api.exception.ErrorCode.BAD_REQUEST_400;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final WithdrawnEmailRepository withdrawnEmailRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final S3ImgService s3Service;
    private final UserReferencedEntitiesDeleteService userReferencedEntitiesDeleteService;
    private final PunishedUserRepository punishedUserRepository;

    public boolean checkDuplicatedNickname(String nickname) {
        return userRepository.findByNickname(nickname).isPresent();
    }

    @Transactional
    public UserDto updateNickname(String email, String nickname) {
        User user = getUser(email);
        user.updateNickname(nickname);
        return getUserDto(user);
    }

    public boolean checkIsSocial(String email) {
        User user = getUser(email);
        return user.getPassword() == null;
    }

    public boolean checkPassword(String email, String password) {
        User user = getUser(email);
        return bCryptPasswordEncoder.matches(password, user.getPassword());
    }

    public void verifyPasswordResetToken(String email, String resetToken) {
        PasswordResetToken resetTokenEntity = resetTokenRepository.findById(resetToken)
                .orElseThrow(() -> new CustomException(BAD_REQUEST_400));
        if (!resetTokenEntity.email().equals(email))
            throw new CustomException(BAD_REQUEST_400);
    }

    @Transactional
    public void updatePassword(String email, String password) {
        User user = getUser(email);
        user.updatePassword(bCryptPasswordEncoder.encode(password));
    }

    public UserDto getMe(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        return user == null ? null : getUserDto(user);
    }

    @Transactional
    public void join(JoinRequest request) {
        if (withdrawnEmailRepository.findById(request.email()).isPresent())
            throw new CustomException(ErrorCode.WITHDRAWN_USER_403);
        userRepository.findByEmail(request.email()).ifPresent(
                user -> {
                    throw new CustomException(ErrorCode.DUPLICATED_EMAIL_400);
                }
        );
        userRepository.findByNickname(request.nickname()).ifPresent(
                user -> {
                    throw new CustomException(ErrorCode.DUPLICATED_NICKNAME_400);
                }
        );
        User user = userRepository.save(
                request.toEntity(bCryptPasswordEncoder.encode(request.password())));
        userPreferencesRepository.save(UserPreferences.of(user));
    }

    @Transactional
    public void delete(String email, WithdrawalReasonDto withdrawalDto) {
        User user = getUser(email);
        userReferencedEntitiesDeleteService.deleteUserReferencedEntities(user.getId());
        userRepository.deleteById(user.getId());
        withdrawnEmailRepository.save(WithdrawnUserEmail.builder()
                .email(user.getEmail())
                .expiration(Duration.ofDays(7).toSeconds())
                .build());
        withdrawalReasonRepository.save(
                WithdrawalReason.builder()
                        .userId(user.getId())
                        .withdrawalCategory(withdrawalDto.category())
                        .description(withdrawalDto.description())
                        .build()
        );
    }

    public UserRankingDto getUserRanking(String email) {
        User user = getUser(email);
        List<RankedUserDto> rankers = userRepository.findTopRankedUsers(PageRequest.of(0, 10));
        List<RankedUserDto> rankedUserDtos = rankers.stream().map(ranker ->
                new RankedUserDto(
                        ranker.id(), ranker.nickname(), ranker.earnedBetSucceedPoint(), s3Service.generateUserImgUrlOrNull(ranker.id())
                        )).toList();
        Integer userRank = userRepository.findRankByPoint(user.getEarnedBetSucceedPoint());
        return new UserRankingDto(userRank, rankedUserDtos);
    }

    private UserDto getUserDto(User user) {
        Optional<PunishedUser> punishedUserOpt = punishedUserRepository.findById(user.getId());
        String profileImgUrl = s3Service.generateUserImgUrlOrNull(user.getId());
        if (punishedUserOpt.isPresent()) {
            PunishedUser punishedUser = punishedUserOpt.get();
            LocalDateTime restrictEndAt = LocalDateTime.now().plusSeconds(punishedUser.expiration());
            return UserDto.toDto(user, profileImgUrl, punishedUser.reportCategory(), restrictEndAt);
        }
        return UserDto.toDto(user, profileImgUrl, null, null);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new CustomException(BAD_REQUEST_400));
    }

}
