package my.mma.admin.event.service;

import my.mma.admin.event.dto.CrawledFightCard;
import my.mma.admin.event.dto.CrawledPrevEvent;
import my.mma.admin.event.dto.CrawledUpcomingEvent;
import my.mma.admin.event.dto.CrawledUpcomingEvent.EventCrawlerDto;
import my.mma.api.fighter.dto.CareerStatsDto;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.global.fcm.AdminPushNotificationService;
import my.mma.fixture.entity.fighter.FighterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static my.mma.fixture.entity.fighter.FighterFixture.createFighterWithName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlaskEventServiceTest {

    @InjectMocks
    private FlaskEventService flaskEventService;

    @Mock
    private FighterRepository fighterRepository;

    @Mock
    private FightEventRepository fightEventRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AdminPushNotificationService adminPushNotificationService;

    private static final String FLASK_URI = "http://flask";
    private static final LocalDate EVENT_DATE = LocalDate.of(2026, 4, 19);
    private static final String EVENT_DATE_STR = "April 19, 2026";
    private static final String EVENT_NAME = "UFC 314";

    /**
     * 기본 5개 카드 (main card 기준)
     * 인덱스 0 = 메인이벤트, 4 = 첫 번째 카드
     */
    private static final List<String[]> BASE_CARDS = List.of(
            new String[]{"Poirier", "Gaethje"},
            new String[]{"Oliveira", "Makhachev"},
            new String[]{"Adesanya", "Pereira"},
            new String[]{"Ngannou", "Jones"},
            new String[]{"Volkanovski", "Holloway"}
    );

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(flaskEventService, "flaskURI", FLASK_URI);
    }

    // --- Helpers ---

    private CrawledFightCard card(String winner, String loser) {
        return new CrawledFightCard(winner, loser, null, null, "Lightweight", false, false, false, null, null, false, false);
    }

    /**
     * BASE_CARDS 기반 5개 CrawledFightCard 생성
     */
    private List<CrawledFightCard> baseCards() {
        return BASE_CARDS.stream().map(p -> card(p[0], p[1])).toList();
    }

    private EventCrawlerDto eventDto(List<CrawledFightCard> cards) {
        CardStartDateTimeInfoDto mainCardInfo = CardStartDateTimeInfoDto.builder()
                .date(EVENT_DATE)
                .time(LocalTime.of(6, 0))
                .build();
        return new EventCrawlerDto(EVENT_NAME, EVENT_DATE_STR, mainCardInfo, null, null, 5, 5, 2, "Las Vegas", cards);
    }

    private FightEvent existingEventWith(List<FighterFightEvent> ffes) {
        FightEvent event = FightEvent.builder()
                .id(1L)
                .name(EVENT_NAME)
                .eventDate(EVENT_DATE)
                .displayDate(EVENT_DATE)
                .completed(false)
                .location("Las Vegas")
                .build();
        ffes.forEach(event::addFighterFightEvent);
        return event;
    }

    /**
     * BASE_CARDS 기반 5개 활성 FFE를 가진 기존 이벤트
     * cardOrder를 역순(5,4,3,2,1)으로 세팅 — updateUpcomingEventsContent가 호출되면 1,2,3,4,5로 바뀌므로 변화 감지 가능
     */
    private FightEvent existingEventWithBaseCards() {
        int totalCards = BASE_CARDS.size();
        List<FighterFightEvent> ffes = new ArrayList<>();
        for (int i = 0; i < totalCards; i++) {
            String[] p = BASE_CARDS.get(i);
            ffes.add(upcomingFFEWithOrder(createFighterWithName(p[0]), createFighterWithName(p[1]), totalCards - i));
        }
        return existingEventWith(ffes);
    }

    private FighterFightEvent upcomingFFE(Fighter winner, Fighter loser) {
        return upcomingFFEWithOrder(winner, loser, 1);
    }

    private FighterFightEvent upcomingFFEWithOrder(Fighter winner, Fighter loser, int cardOrder) {
        return FighterFightEvent.builder()
                .winner(winner)
                .loser(loser)
                .fightWeight(FightWeight.LIGHTWEIGHT)
                .cardOrder(cardOrder)
                .title(false)
                .canceled(false)
                .build();
    }

    private FighterFightEvent canceledFFE(Fighter winner, Fighter loser) {
        return FighterFightEvent.builder()
                .winner(winner)
                .loser(loser)
                .fightWeight(FightWeight.LIGHTWEIGHT)
                .cardOrder(1)
                .title(false)
                .canceled(true)
                .build();
    }

    /**
     * set crawledUpcomingEventDTO & let fighterRepository returns fighters included in crawledEvent's names
     */
    private void givenCrawledUpcomingFightEventReturns(List<CrawledFightCard> cards) {
        List<String> names = cards.stream()
                .flatMap(c -> Stream.of(c.winnerName(), c.loserName()))
                .distinct().toList();

        CrawledUpcomingEvent crawledEvent = new CrawledUpcomingEvent(
                names.stream().map(name -> new CrawledUpcomingEvent.FighterCrawlerDto(
                        name, "10-1-0", "155", "180", "nick", "70", "Jan 01, 1990",
                        new CareerStatsDto(
                                1.1, 1.1, 1.1, 1.1, 1, 1, 1, 1)
                )).toList(),
                List.of(eventDto(cards))
        );
        given(restTemplate.getForObject(eq(FLASK_URI + "/upcoming_event"), eq(CrawledUpcomingEvent.class)))
                .willReturn(crawledEvent);
        given(fighterRepository.findAllByNameIn(anyList()))
                .willReturn(names.stream().map(FighterFixture::createFighterWithName).toList());
    }

    @Nested
    @DisplayName("새 이벤트")
    class NewEvent {

        @Test
        @DisplayName("크롤링 시 가져온 Upcoming가 DB에 존재하지 않는 경우, 해당 이벤트를 DB에 저장한다.")
        void savesNewEvent() {
            givenCrawledUpcomingFightEventReturns(baseCards());
            given(fightEventRepository.findByCompletedIsFalse()).willReturn(List.of());
            flaskEventService.syncFightEvents();
            verify(fightEventRepository).save(any(FightEvent.class));
        }
    }

    @Nested
    @DisplayName("기존 이벤트 카드 변경 감지")
    class ExistingEventUpdate {

        static Stream<Arguments> removedCardIndexProvider() {
            // return Stream<>(index, fighter1name[index], fighter2name[index])
            return IntStream.range(0, BASE_CARDS.size())
                    .mapToObj(i -> Arguments.of(
                            i, BASE_CARDS.get(i)[0], BASE_CARDS.get(i)[1]));
        }

        @Test
        @DisplayName("크롤링 시 가져온 UpcomingEvent 카드 구성이 DB의 구성과 동일하면 이벤트를 수정하지 않는다")
        void noUpdateWhenUnchanged() {
            FightEvent existing = existingEventWithBaseCards();
            givenCrawledUpcomingFightEventReturns(baseCards());
            given(fightEventRepository.findByCompletedIsFalse()).willReturn(List.of(existing));

            flaskEventService.syncFightEvents();

            List<FighterFightEvent> ffes = existing.getFighterFightEvents();
            assertThat(ffes).hasSize(5);
            assertThat(ffes.stream().noneMatch(FighterFightEvent::isCanceled)).isTrue();
            // cardOrder가 역순(5,4,3,2,1) 그대로면 updateUpcomingEventsContent가 호출되지 않은 것
            assertThat(ffes.stream().map(FighterFightEvent::getCardOrder).toList())
                    .containsExactly(5, 4, 3, 2, 1);
        }

        @Test
        @DisplayName("새 카드가 추가되면 winner/loser 이름이 올바르게 저장된다")
        void addsNewFFEWithCorrectWinnerAndLoser() {
            //given
            FightEvent existing = existingEventWithBaseCards();

            List<CrawledFightCard> sixCards = new ArrayList<>(baseCards());
            sixCards.add(card("NewFighter-W", "NewFighter-L"));
            givenCrawledUpcomingFightEventReturns(sixCards);
            given(fightEventRepository.findByCompletedIsFalse()).willReturn(List.of(existing));

            //when
            flaskEventService.syncFightEvents();

            //then
            List<FighterFightEvent> ffes = existing.getFighterFightEvents();
            assertThat(ffes).hasSize(6);
            assertThat(ffes.stream().noneMatch(FighterFightEvent::isCanceled)).isTrue();

            FighterFightEvent newFFE = ffes.stream()
                    .filter(f -> f.getWinner().getName().equals("NewFighter-W"))
                    .findFirst().orElseThrow();
            assertThat(newFFE.getLoser().getName()).isEqualTo("NewFighter-L");
            assertThat(newFFE.getWinner().getName()).isNotEqualTo(newFFE.getLoser().getName());
        }

        @ParameterizedTest(name = "인덱스 {0} ({1} vs {2}) 카드 제거 시 해당 카드만 canceled")
        @MethodSource("removedCardIndexProvider")
        void cancelsOnlyTheRemovedCard(int removedIndex, String expectedWinner, String expectedLoser) {
            FightEvent existing = existingEventWithBaseCards();

            List<CrawledFightCard> fourCards = new ArrayList<>(baseCards());
            fourCards.remove(removedIndex);
            // crawled card returns four cards
            givenCrawledUpcomingFightEventReturns(fourCards);
            // while repository returns five cards (means one card is canceled)
            given(fightEventRepository.findByCompletedIsFalse()).willReturn(List.of(existing));

            flaskEventService.syncFightEvents();

            List<FighterFightEvent> ffes = existing.getFighterFightEvents();
            assertThat(ffes.stream().filter(FighterFightEvent::isCanceled)).hasSize(1);
            FighterFightEvent canceled = ffes.stream().filter(FighterFightEvent::isCanceled).findFirst().orElseThrow();
            assertThat(canceled.getWinner().getName()).isEqualTo(expectedWinner);
            assertThat(canceled.getLoser().getName()).isEqualTo(expectedLoser);
        }
    }

    @Nested
    @DisplayName("canceled 카드 재등장")
    class CanceledCardReactivation {

        @Test
        @DisplayName("5개 카드 중 canceled 카드가 크롤링에 다시 나타나면 reactivate한다")
        void reactivatesCanceledFFE() {
            int canceledIdx = 2;
            // 4개 활성 + "Adesanya" vs "Pereira" canceled
            List<FighterFightEvent> ffes = new ArrayList<>();
            for (int i = 0; i < BASE_CARDS.size(); i++) {
                String[] p = BASE_CARDS.get(i);
                Fighter w = createFighterWithName(p[0]);
                Fighter l = createFighterWithName(p[1]);
                ffes.add(i == canceledIdx ? canceledFFE(w, l) : upcomingFFE(w, l));
            }
            FightEvent existing = existingEventWith(ffes);
            FighterFightEvent canceledFfe = ffes.get(canceledIdx);

            // 크롤링에는 5개 모두 포함 (canceled 카드 복귀)
            givenCrawledUpcomingFightEventReturns(baseCards());
            given(fightEventRepository.findByCompletedIsFalse()).willReturn(List.of(existing));

            flaskEventService.syncFightEvents();

            assertThat(canceledFfe.isCanceled()).isFalse();
            assertThat(existing.getFighterFightEvents().stream().noneMatch(FighterFightEvent::isCanceled)).isTrue();
        }

        @Test
        @DisplayName("canceled 카드 재등장 시 중복 FFE를 생성하지 않는다")
        void doesNotDuplicateCanceledFFEOnReactivation() {
            List<FighterFightEvent> ffes = new ArrayList<>();
            for (int i = 0; i < BASE_CARDS.size(); i++) {
                String[] p = BASE_CARDS.get(i);
                Fighter w = createFighterWithName(p[0]);
                Fighter l = createFighterWithName(p[1]);
                ffes.add(i == 2 ? canceledFFE(w, l) : upcomingFFE(w, l));
            }
            FightEvent existing = existingEventWith(ffes);

            givenCrawledUpcomingFightEventReturns(baseCards());
            given(fightEventRepository.findByCompletedIsFalse()).willReturn(List.of(existing));

            flaskEventService.syncFightEvents();

            // canceled 포함 총 5개여야 함 (중복 생성 없음)
            assertThat(existing.getFighterFightEvents()).hasSize(5);
            long adesanyaCount = existing.getFighterFightEvents().stream()
                    .filter(f -> f.getWinner().getName().equals("Adesanya"))
                    .count();
            assertThat(adesanyaCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("과거 이벤트 처리")
    class PastEventHandling {

        @Test
        @DisplayName("DB에 있던 이벤트가 크롤링에서 사라지면 completed로 마킹한다")
        void marksEventAsCompletedWhenMissingFromCrawl() {
            // 현재 크롤링에는 UFC 314(다른 날짜)만 존재
            givenCrawledUpcomingFightEventReturns(baseCards());
            // UFC 313: 다른 날짜 이벤트 (크롤링에서 사라진 상태)
            FightEvent pastEvent = FightEvent.builder()
                    .id(2L)
                    .name("UFC 313")
                    .eventDate(LocalDate.of(2026, 4, 5))
                    .displayDate(LocalDate.of(2026, 4, 5))
                    .completed(false)
                    .location("Las Vegas")
                    .build();
            BASE_CARDS.forEach(p ->
                    pastEvent.addFighterFightEvent(
                            upcomingFFE(createFighterWithName(p[0]), createFighterWithName(p[1]))));
            given(fightEventRepository.findByCompletedIsFalse()).willReturn(List.of(pastEvent));

            List<CrawledFightCard> prevResultCards = BASE_CARDS.stream()
                    .map(p -> new CrawledFightCard(p[0], p[1], null, null, "Lightweight", false, false, false, "3", "5:00", false, false))
                    .toList();
            given(restTemplate.getForObject(contains("/prev_event"), eq(CrawledPrevEvent.class)))
                    .willReturn(new CrawledPrevEvent("UFC 313", prevResultCards));

            flaskEventService.syncFightEvents();

            assertThat(pastEvent.isCompleted()).isTrue();
        }
    }
}
