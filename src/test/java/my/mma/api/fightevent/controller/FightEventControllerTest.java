package my.mma.api.fightevent.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.mma.api.config.TestSecurityConfig;
import my.mma.api.fightevent.dto.FightEventDto;
import my.mma.api.fightevent.dto.FightEventDto.FighterFightEventDto;
import my.mma.api.fightevent.dto.FighterFightEventCardDetailDto;
import my.mma.api.fightevent.service.FightEventService;
import my.mma.fixture.dto.fightevent.FightEventDtoFixture;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static my.mma.fixture.dto.auth.CustomUserDetailsFixture.createCustomUserDetails;
import static my.mma.fixture.dto.fightevent.FighterFightEventCardDetailDtoFixture.createFighterFightEventCardDetailDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FightEventController.class)
@Import(TestSecurityConfig.class)
class FightEventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FightEventService fightEventService;

    private final String urlPrefix = "/event";

    @Test
    @DisplayName("날짜를 입력받았을 때 해당 날짜에 해당하는 파이트 이벤트 정보 반환")
    void fightEventDetail() throws Exception {
        //given
        String headshotUrl = "img-url";
        LocalDate givenDate = LocalDate.of(2000, 10, 10);
        FightEventDto fightEventDto = FightEventDtoFixture.createUpcomingFightEventDtoWithIdAndDisplayDate(1, headshotUrl, givenDate);
        when(fightEventService.getSchedule(eq(givenDate), anyString())).thenReturn(List.of(fightEventDto));

        //when
        MvcResult mvcResult = mockMvc.perform(get(urlPrefix + "/detail?date=" + givenDate)
                        .with(authentication(new UsernamePasswordAuthenticationToken(createCustomUserDetails(), null))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
        List<FightEventDto> responseBody = objectMapper.readValue(responseBodyAsString, new TypeReference<>() {
        });

        //then
        assertThat(responseBody).hasSize(1);
        assertThat(responseBody.getFirst()).usingRecursiveComparison().isEqualTo(fightEventDto);
        assertThat(responseBody.getFirst().getFighterFightEvents().size()).isEqualTo(fightEventDto.getFighterFightEvents().size());
    }

    @Test
    @DisplayName("검색된 이름이 해당되는 메인 카드 페이징 정보 반환")
    void fightEventSearch() throws Exception {
        //given
        String imgUrl = "img-url";
        int range = 20;
        int pageSize = 10;
        Pageable pageable = PageRequest.of(0, pageSize);
        Page<FighterFightEventDto> mainCardsDto = getFightEventPage(range, pageable, imgUrl);
        when(fightEventService.search(anyString(), any(Pageable.class))).thenReturn(mainCardsDto);

        //when && then
        MvcResult mvcResult = mockMvc.perform(get(urlPrefix + "/events?name={}", "name"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
        PageImplExceptPageableAndSort<FighterFightEventDto> responseBody =
                objectMapper.readValue(responseBodyAsString, new TypeReference<>() {
                });

        //then
        assertThat(mainCardsDto.getSize()).isEqualTo(responseBody.size());
        assertThat(mainCardsDto.getNumberOfElements()).isEqualTo(responseBody.numberOfElements());
        assertThat(mainCardsDto.getTotalElements()).isEqualTo(responseBody.totalElements());
    }

    @Test
    @DisplayName("입력받은 cardId의 상세 정보 반환")
    void fighterFightEventCardDetail() throws Exception {
        //given
        Long cardId = 1L;
        FighterFightEventCardDetailDto cardDetailDto = createFighterFightEventCardDetailDto();
        when(fightEventService.cardDetail(cardId)).thenReturn(cardDetailDto);

        //when && then
        MvcResult mvcResult = mockMvc.perform(get(urlPrefix + "/card/detail?cardId=" + cardId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andReturn();
        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
        FighterFightEventCardDetailDto responseBody =
                objectMapper.readValue(responseBodyAsString, FighterFightEventCardDetailDto.class);

        //then
        assertThat(responseBody.winner()).usingRecursiveComparison().isEqualTo(cardDetailDto.winner());
    }

//    @DisplayName("파이트 이벤트의 알림 설정 여부를 toggle한다.")
//    @Test
//    void updateFightEventPreferenceTest() throws Exception {
//        //given
//        UpdateAlertRequest updateAlertRequest = new UpdateAlertRequest(2L, true, UPCOMING_EVENT);
//        doNothing().when(updatePreferenceService).updateAlert(anyString(), eq(updateAlertRequest));
//
//        //when && then
//        MvcResult mvcResult = mockMvc.perform(post(urlPrefix)
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

    private Page<FighterFightEventDto> getFightEventPage(int range, Pageable pageable, String imgUrl) {
        List<FighterFightEventDto> fighterFightEventsDto = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            FightEventDto fightEvent = FightEventDtoFixture.createUpcomingFightEventDtoWithIdAndDisplayDate(i, imgUrl, LocalDate.now());
            fighterFightEventsDto.add(fightEvent.getFighterFightEvents().get(0));
        }
        return new PageImpl<>(fighterFightEventsDto, pageable, fighterFightEventsDto.size());
    }

}