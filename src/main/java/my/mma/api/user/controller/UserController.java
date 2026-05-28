package my.mma.api.user.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.bet.dto.BetResponse;
import my.mma.api.bet.service.UserRecentBetHistoryService;
import my.mma.api.security.CustomUserDetails;
import my.mma.api.user.dto.*;
import my.mma.api.user.service.UserProfileImageUploadService;
import my.mma.api.user.service.UserProfileService;
import my.mma.api.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final UserProfileImageUploadService userProfileImageUploadService;
    private final UserRecentBetHistoryService betHistoryService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().body(userService.getMe(userDetails.getUsername()));
    }

    @GetMapping("/dup_nickname")
    public ResponseEntity<Boolean> checkDuplicatedNickname(@RequestBody Map<String, String> nicknameMap) {
        return ResponseEntity.ok(userService.checkDuplicatedNickname(nicknameMap.get("nickname")));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<UserDto> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Validated @RequestBody NicknameUpdateRequest request
    ) {
        return ResponseEntity.ok().body(userService.updateNickname(userDetails.getUsername(), request.nickname()));
    }

    @GetMapping("/is_social")
    public ResponseEntity<Boolean> checkIsSocial(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok().body(userService.checkIsSocial(userDetails.getUsername()));
    }

    @GetMapping("/password")
    public ResponseEntity<Boolean> checkPassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> passwordMap
    ) {
        return ResponseEntity.ok().body(userService.checkPassword(userDetails.getUsername(), passwordMap.get("password")));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> passwordMap
    ) {
        userService.updatePassword(userDetails.getUsername(), passwordMap.get("password"));
        return ResponseEntity.ok().body(null);
    }

    // mapper for user who's finding password
    @PatchMapping("/password-reset")
    public ResponseEntity<Void> resetPassword(
            @RequestBody @Validated ResetPasswordRequest request
    ) {
        userService.verifyPasswordResetToken(request.email(), request.resetToken());
        userService.updatePassword(request.email(), request.password());
        return ResponseEntity.ok().body(null);
    }

    @PostMapping("")
    public ResponseEntity<Void> join(@RequestBody @Validated JoinRequest request) {
        userService.join(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @DeleteMapping("")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody WithdrawalReasonDto withdrawalDto
    ) {
        userService.delete(userDetails.getUsername(), withdrawalDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
    }

    @GetMapping("/ranking")
    public ResponseEntity<UserRankingDto> userRanking(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok().body(userService.getUserRanking(userDetails.getUsername()));
    }

    @GetMapping("/{id}/bet_history")
    public ResponseEntity<List<BetResponse>> userRecentBetHistory(
            @PathVariable("id") Long userId
    ) {
        return ResponseEntity.ok().body(betHistoryService.userBetHistory(userId));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> profile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ResponseEntity.ok().body(userProfileService.profile(customUserDetails.getUsername()));
    }

    @PutMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestPart("image") MultipartFile multipartFile
    ) {
        return ResponseEntity.ok().body(
                userProfileImageUploadService.uploadProfileImage(customUserDetails.getUsername(), multipartFile));
    }

    @DeleteMapping("image")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        userProfileImageUploadService.deleteProfileImage(customUserDetails.getUsername());
        return ResponseEntity.ok().body(null);
    }

}
