package my.mma.api.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.mma.api.bet.service.UserRecentBetHistoryService;
import my.mma.api.config.TestSecurityConfig;
import my.mma.api.user.dto.JoinRequest;
import my.mma.api.user.dto.NicknameUpdateRequest;
import my.mma.api.user.dto.UserDto;
import my.mma.api.user.service.UserProfileImageUploadService;
import my.mma.api.user.service.UserProfileService;
import my.mma.api.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static my.mma.fixture.dto.auth.CustomUserDetailsFixture.AUTH_EMAIL;
import static my.mma.fixture.dto.auth.CustomUserDetailsFixture.createCustomUserDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private UserProfileService userProfileService;
    @MockBean
    private UserProfileImageUploadService userProfileImageUploadService;
    @MockBean
    private UserRecentBetHistoryService userRecentBetHistoryService;

    private final String urlPrefix = "/user";

    private final String validEmail = "email123@google.com";
    private final String validNickname = "nick123";
    private final String validPassword = "pwd123";

    @DisplayName("닉네임 변경 요청(Map<String,String> nicknameMap) 및 응답(UserDto)")
    @Test
    void updateNicknameTest() throws Exception {
        //given
        NicknameUpdateRequest nicknameUpdateRequest = new NicknameUpdateRequest(validNickname);
        UserDto userDto = getUserDto();
        Mockito.when(userService.updateNickname(AUTH_EMAIL, validNickname))
                .thenReturn(userDto);

        //when && then
        MvcResult mvcResult = mockMvc.perform(patch(urlPrefix + "/nickname")
                        .with(authentication(new UsernamePasswordAuthenticationToken(createCustomUserDetails(), null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nicknameUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        String responsBodyAsString = mvcResult.getResponse().getContentAsString();
        UserDto responseBody = objectMapper.readValue(responsBodyAsString, UserDto.class);

        //then
        assertUserDto(userDto, responseBody);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "10111111111", ""})
    @DisplayName("닉네임 변경 시 길이가 2 미만, 10 초과인 경우 validation exception 발생")
    void updateInvalidNicknameTest(String invalidNickname) throws Exception {
        //given
        NicknameUpdateRequest nicknameUpdateRequest = new NicknameUpdateRequest(invalidNickname);
        //when && then
        mockMvc.perform(patch(urlPrefix + "/nickname")
                        .with(authentication(new UsernamePasswordAuthenticationToken(createCustomUserDetails(), null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nicknameUpdateRequest)))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @DisplayName("사용자 기본 정보(UserDto) 불러오기")
    @Test
    void getMeTest() throws Exception {
        //given
        UserDto userDto = getUserDto();
        Mockito.when(userService.getMe(AUTH_EMAIL)).thenReturn(userDto);

        //when && then
        MvcResult mvcResult = mockMvc.perform(get(urlPrefix + "/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(createCustomUserDetails(), null)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String responsBodyAsString = mvcResult.getResponse().getContentAsString();
        UserDto responseBody = objectMapper.readValue(responsBodyAsString, UserDto.class);

        //then
        assertUserDto(userDto, responseBody);
    }

    @DisplayName("회원가입 정상 처리")
    @Test
    void joinTest() throws Exception {
        //given
        JoinRequest joinRequest = new JoinRequest(validEmail, validNickname, validPassword);
        Mockito.doNothing().when(userService).join(joinRequest);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post(urlPrefix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        //then
        assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
    }

    @DisplayName("회원가입 예외 발생 (InvalidEmailException)")
    @Test
    void joinTest_InvalidEmailException() throws Exception {
        //given
        JoinRequest joinRequest = new JoinRequest("email.com", validNickname, validPassword);
        Mockito.doNothing().when(userService).join(joinRequest);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post(urlPrefix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().is4xxClientError())
                .andReturn();

        //then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
    }

    @DisplayName("회원가입 예외 발생 (InvalidNicknameException)")
    @Test
    void joinTest_InvalidNicknameException() throws Exception {
        //given
        JoinRequest joinRequest = new JoinRequest(validEmail, "nickname12345678", validPassword);
        Mockito.doNothing().when(userService).join(joinRequest);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post(urlPrefix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().is4xxClientError())
                .andReturn();

        //then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
    }

    @DisplayName("회원가입 예외 발생 (InvalidPasswordException)")
    @Test
    void joinTest_InvalidPasswordException() throws Exception {
        //given
        JoinRequest joinRequest = new JoinRequest(validEmail, validNickname, "pwd");
        Mockito.doNothing().when(userService).join(joinRequest);

        //when && then
        MvcResult mvcResult = mockMvc.perform(post(urlPrefix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().is4xxClientError())
                .andReturn();

        //then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
    }

    private UserDto getUserDto() {
        return UserDto.builder()
                .id(1L)
                .email(validEmail)
                .nickname(validNickname)
                .point(0)
                .role("ROLE_USER")
                .build();
    }

    private void assertUserDto(UserDto userDto, UserDto responseBody) {
        assertThat(userDto.id()).isEqualTo(responseBody.id());
        assertThat(userDto.nickname()).isEqualTo(responseBody.nickname());
        assertThat(userDto.email()).isEqualTo(responseBody.email());
        assertThat(userDto.role()).isEqualTo(responseBody.role());
        assertThat(userDto.point()).isEqualTo(responseBody.point());
    }

}