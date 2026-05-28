package my.mma.api.user.service;

import my.mma.api.alert.entity.UserPreferences;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.user.dto.JoinRequest;
import my.mma.api.user.dto.UserDto;
import my.mma.api.user.entity.User;
import my.mma.api.alert.repository.UserPreferencesRepository;
import my.mma.api.user.repository.PunishedUserRepository;
import my.mma.api.user.repository.UserRepository;
import my.mma.api.user.repository.WithdrawnEmailRepository;
import my.mma.api.user.entity.WithdrawnUserEmail;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WithdrawnEmailRepository withdrawnEmailRepository;
    @Mock
    private UserPreferencesRepository userPreferencesRepository;
    @Mock
    private PunishedUserRepository punishedUserRepository;
    @Mock
    private S3ImgService s3Service;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final String nickname = "nickname123";
    private final String email = "email123@google.com";
    private final String password = "pwd123";
    private User user;

    @BeforeEach
    void setupUser(){
        user = User.builder()
                .id(1L)
                .nickname(nickname)
                .email(email)
                .point(0)
                .role("ROLE_USER")
                .password(password)
                .build();
    }

    @Test
    @DisplayName("닉네임 변경 정상 처리")
    void updateNicknameTest(){
        //given
        String nicknameForUpdate = "updatedNickname123";
        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        //when
        UserDto userDto = userService.updateNickname(email, nicknameForUpdate);

        //then
        assertThat(userDto.nickname()).isNotEqualTo(nickname);
        assertThat(userDto.nickname()).isEqualTo(nicknameForUpdate);
    }

    @Test
    @DisplayName("기본 사용자 정보 반환 정상 처리")
    void getMeTest(){
        //given
        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        //when
        UserDto userDto = userService.getMe(email);

        //then
        assertThat(userDto.id()).isEqualTo(user.getId());
        assertThat(userDto.nickname()).isEqualTo(user.getNickname());
        assertThat(userDto.email()).isEqualTo(user.getEmail());
        assertThat(userDto.role()).isEqualTo(user.getRole());
    }

    @Test
    @DisplayName("회원가입 정상 처리 (암호화 메서드가 정확히 1회 호출된다.)")
    void joinTest_valid(){
        //given
        Mockito.when(passwordEncoder.encode(password)).thenReturn(password);
        Mockito.when(userRepository.save(any(User.class))).thenReturn(user);
        JoinRequest joinRequest = new JoinRequest(email, nickname, password);

        //when
        userService.join(joinRequest);

        //then
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    @DisplayName("회원가입 예외 발생 (탈퇴한 이메일로 재가입 시도)")
    void joinTest_WITHDRAWN_USER_403(){
        //given
        Mockito.when(withdrawnEmailRepository.findById(email))
                .thenReturn(Optional.of(WithdrawnUserEmail.builder().email(email).expiration(100L).build()));
        JoinRequest joinRequest = new JoinRequest(email, nickname, password);

        //expect
        Assertions.assertThatThrownBy(() -> userService.join(joinRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.WITHDRAWN_USER_403.name());
    }

    @Test
    @DisplayName("회원가입 예외 발생 (중복된 이메일)")
    void joinTest_BAD_REQUEST_400_중복된_이메일(){
        //given
        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        JoinRequest joinRequest = new JoinRequest(email,nickname,password);

        //expect
        Assertions.assertThatThrownBy(() -> userService.join(joinRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.DUPLICATED_EMAIL_400.name());
    }

    @Test
    @DisplayName("회원가입 예외 발생 (중복된 닉네임)")
    void joinTest_BAD_REQUEST_400_중복된_닉네임(){
        //given
        Mockito.when(userRepository.findByNickname(nickname)).thenReturn(Optional.of(user));
        JoinRequest joinRequest = new JoinRequest(email,nickname,password);

        //expect
        Assertions.assertThatThrownBy(() -> userService.join(joinRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.DUPLICATED_NICKNAME_400.name());
    }

}
