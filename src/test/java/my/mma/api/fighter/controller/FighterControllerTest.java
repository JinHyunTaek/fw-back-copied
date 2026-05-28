package my.mma.api.fighter.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.mma.api.alert.service.AlertService;
import my.mma.api.config.TestSecurityConfig;
import my.mma.api.fighter.dto.FighterDetailDto;
import my.mma.api.fighter.dto.FighterDto;
import my.mma.api.fighter.service.FighterService;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.fixture.page.PageImplExceptPageableAndSort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static my.mma.fixture.dto.auth.CustomUserDetailsFixture.AUTH_EMAIL;
import static my.mma.fixture.dto.auth.CustomUserDetailsFixture.createCustomUserDetails;
import static my.mma.fixture.dto.fighter.FighterDetailDtoFixture.createFighterDetailDto;
import static my.mma.fixture.dto.fighter.FighterDtoFixture.createFighterDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FighterController.class)
@Import(TestSecurityConfig.class)
class FighterControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FighterService fighterService;
    @MockBean
    private S3ImgService s3Service;
    @MockBean
    private AlertService updatePreferenceService;

    private final String urlPrefix = "/fighter";

    @Test
    @DisplayName("파이터 상세 정보(FighterDetailDto) 반환")
    void fighterDetail() throws Exception {
        //given
        Long fighterId = 1L;
        String imgUrl = "img-url";
        FighterDetailDto fighterDetailDto = createFighterDetailDto(fighterId, imgUrl);
        when(fighterService.detail(AUTH_EMAIL, fighterId)).thenReturn(fighterDetailDto);

        //when && then
        MvcResult mvcResult = mockMvc.perform(get(urlPrefix + "/{fighterId}", fighterId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(createCustomUserDetails(), null)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
        FighterDetailDto responseBody = objectMapper.readValue(responseBodyAsString, FighterDetailDto.class);

        //then
        assertThat(fighterDetailDto).usingRecursiveComparison().isEqualTo(responseBody);
    }

    @Test
    @DisplayName("파이터 페이징 정보 반환")
    void fighterSearch() throws Exception {
        //given
        String imgUrl = "img-url";
        int range = 20;
        int pageSize = 10;
        Pageable pageable = PageRequest.of(0, pageSize);
        Page<FighterDto> fighterDtoPage = getFightersPage(range, imgUrl, pageable);
        when(fighterService.search(anyString(), any(Pageable.class))).thenReturn(fighterDtoPage);

        //when && then
        MvcResult mvcResult = mockMvc.perform(get(urlPrefix + "/fighters?name={}", "name")
                        .with(authentication(new UsernamePasswordAuthenticationToken(createCustomUserDetails(), null)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
        PageImplExceptPageableAndSort<FighterDto> responseBody = objectMapper.readValue(responseBodyAsString, new TypeReference<>() {
        });

        //then
        assertThat(fighterDtoPage.getSize()).isEqualTo(responseBody.size());
        assertThat(fighterDtoPage.getNumberOfElements()).isEqualTo(responseBody.numberOfElements());
        assertThat(fighterDtoPage.getTotalElements()).isEqualTo(responseBody.totalElements());
    }

//    @DisplayName("파이터의 알림 설정 여부를 toggle한다.")
//    @Test
//    void updateFighterPreferenceTest() throws Exception {
//        //given
//        UpdateAlertRequest updateAlertRequest = new UpdateAlertRequest(2L, true, FIGHTER);
//        doNothing().when(updatePreferenceService).updateSingleAlert(anyString(), eq(updateAlertRequest));
//
//        //when && then
//        MvcResult mvcResult = mockMvc.perform(post(urlPrefix + "/''")
//                        .with(authentication(new UsernamePasswordAuthenticationToken(createCustomUserDetails(), null)))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updateAlertRequest)))
//                .andExpect(answered().isOk())
//                .andReturn();
//        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
//
//        //then
//        assertThat(responseBodyAsString).isEmpty();
//    }

    private Page<FighterDto> getFightersPage(int range, String imgUrl, Pageable pageable) {
        List<FighterDto> fightersDto = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            fightersDto.add(createFighterDto((long) i, imgUrl));
        }
        return new PageImpl<>(fightersDto, pageable, fightersDto.size());
    }

}