package my.mma.api.bet.service;

import my.mma.api.bet.dto.BetDeleteResponse;
import my.mma.api.bet.dto.SingleBetRequest;
import my.mma.api.bet.dto.SingleBetRequest.SingleBetCardRequest;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.entity.BetPrediction;
import my.mma.api.bet.repository.BetCardRepository;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.bet.repository.FightPickCountRepository;
import my.mma.api.exception.CustomException;
import my.mma.api.exception.ErrorCode;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.WinMethod;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.fightevent.repository.FighterFightEventRepository;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import my.mma.fixture.entity.bet.BetCardFixture;
import my.mma.fixture.entity.bet.BetFixture;
import my.mma.fixture.entity.fighterfightevent.FighterFightEventFixture;
import my.mma.fixture.entity.fightevent.FightEventFixture;
import my.mma.fixture.entity.user.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static my.mma.api.bet.service.BetService.BET_AVAILABLE_COUNT;
import static my.mma.api.bet.service.BetService.BET_CANCEL_AVAILABLE_COUNT;
import static my.mma.api.exception.ErrorCode.*;
import static my.mma.api.fightevent.dto.CurrentEventDto.toDto;
import static my.mma.api.global.redis.key.RedisKey.CURRENT_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetServiceTest {

    @InjectMocks
    private BetService betService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private BetRepository betRepository;
    @Mock
    private BetCardRepository betCardRepository;
    @Mock
    private FightEventRepository fightEventRepository;
    @Mock
    private FighterFightEventRepository fighterFightEventRepository;
    @Mock
    private RedisUtils<CurrentEventDto> currentEventRedisUtils;
    @Mock
    private FightPickCountRepository fightPickCountRepository;
    @Mock
    private StringRedisTemplate betCancelCountRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("BetService.bet() test")
    class BetTest {

        private final int FFE_END_RANGE = 6;
        private final int seedPoint = 300;
        private final int userPoint = 500;
        private final User user = UserFixture.builder().point(userPoint).build();
        private final Long eventId = 1L;
        private final FightEvent fightEvent = FightEventFixture.createUpcomingFightEventWithId(eventId);
        private final CurrentEventDto currentEventDto = toDto(fightEvent);
        private final List<FighterFightEvent> ffes = IntStream.range(0, FFE_END_RANGE)
                .mapToObj(i -> FighterFightEventFixture.builder((long) i, (long) i, (long) (i + 10))
                        .fightEvent(fightEvent).build()).toList();

        private final List<SingleBetCardRequest> betCardsRequest = LongStream.range(0, FFE_END_RANGE / 2)
                .mapToObj(i -> new SingleBetCardRequest(i, BetPrediction.builder()
                        .myWinnerId(i)
                        .myLoserId(i + 10)
                        .build())).toList();
        private final SingleBetRequest betRequest = new SingleBetRequest(eventId, seedPoint, betCardsRequest);

        private int prevPoint;

        @BeforeEach
        void setup() {
            prevPoint = user.getPoint();
        }

        @ParameterizedTest
        @MethodSource("existingBetCounts")
        @DisplayName("배팅 성공 시, 사용자 포인트 차감")
        void betSuccess_thenReturnDeductedUserPoint(int betCount) {
            when(userRepository.findByEmailWithLock(user.getEmail())).thenReturn(user);
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(currentEventDto);
            when(fightEventRepository.findByIdWithFfes(eventId)).thenReturn(Optional.ofNullable(fightEvent));
            when(betCardRepository.countByUserIdAndFightEventId(user.getId(), eventId)).thenReturn(betCount);
            for (int i = 0; i < FFE_END_RANGE / 2; i++)
                when(fighterFightEventRepository.findById((long) i)).thenReturn(Optional.ofNullable(ffes.get(i)));

            Integer deductedPoint = betService.predict(user.getEmail(), betRequest);

            assertThat(deductedPoint).isEqualTo(prevPoint - betRequest.seedPoint());
            assertThat(user.getPoint()).isEqualTo(prevPoint - betRequest.seedPoint());
        }

        @Test
        @DisplayName("다음 경기가 redis에 존재하지 않는 경우, NO_SUCH_EVENT_FOUND_400 예외 발생")
        void currentEventNotExists_thenThrow_NO_SUCH_EVENT_FOUND_400() {
            when(userRepository.findByEmailWithLock(user.getEmail())).thenReturn(user);
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(null);

            assertThatThrownBy(() -> betService.predict(user.getEmail(), betRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.NO_SUCH_EVENT_FOUND_400.name());
            assertThat(user.getPoint()).isEqualTo(prevPoint);
        }

        @Test
        @DisplayName("요청한 eventId가 현재 이벤트 id와 다른 경우, NO_SUCH_EVENT_FOUND_400 예외 발생")
        void requestEventIdIsNotCurrentEventId_thenThrow_NO_SUCH_EVENT_FOUND_400() {
            CurrentEventDto wrongEventDto = toDto(fightEvent);
            wrongEventDto.setId(-1L);

            when(userRepository.findByEmailWithLock(user.getEmail())).thenReturn(user);
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(wrongEventDto);

            assertThatThrownBy(() -> betService.predict(user.getEmail(), betRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(NO_SUCH_EVENT_FOUND_400.name());
            assertThat(user.getPoint()).isEqualTo(prevPoint);
        }

        @Test
        @DisplayName("user의 포인트가 seedPoint보다 적은 경우, LOW_USER_POINT_400 예외 발생")
        void userPointLowerThanSeedPoint_thenThrow_LOW_USER_POINT_400() {
            int invalidSeedPoint = userPoint + 100;
            SingleBetRequest invalidBetRequest = new SingleBetRequest(eventId, invalidSeedPoint, betCardsRequest);

            when(userRepository.findByEmailWithLock(user.getEmail())).thenReturn(user);
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(currentEventDto);

            assertThatThrownBy(() -> betService.predict(user.getEmail(), invalidBetRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.LOW_USER_POINT_400.name());
            assertThat(user.getPoint()).isEqualTo(prevPoint);
        }

        @ParameterizedTest
        @MethodSource("pastEventStartDateTimes")
        @DisplayName("이벤트 시작 날짜가 이미 지난 경우, BET_NOT_AVAILABLE_DATE_403 예외 발생")
        void currentEventDateAlreadyPassed_thenThrow_BET_NOT_AVAILABLE_DATE_403(LocalDateTime startedAt) {
            FightEvent startedEvent = FightEventFixture.builder(eventId).earlyDT(startedAt).build();
            CurrentEventDto startedEventDto = CurrentEventDto.toDto(startedEvent);

            when(userRepository.findByEmailWithLock(user.getEmail())).thenReturn(user);
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(startedEventDto);

            assertThatThrownBy(() -> betService.predict(user.getEmail(), betRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(BET_NOT_AVAILABLE_DATE_403.name());
        }

        @Test
        @DisplayName("배팅 한도 초과 시, BET_LIMIT_EXCEED_403 예외 발생")
        void exceedBetLimit_thenThrow_BET_LIMIT_EXCEED_403() {
            when(userRepository.findByEmailWithLock(user.getEmail())).thenReturn(user);
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(currentEventDto);
            when(betCardRepository.countByUserIdAndFightEventId(user.getId(), eventId)).thenReturn(BET_AVAILABLE_COUNT);

            assertThatThrownBy(() -> betService.predict(user.getEmail(), betRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(BET_LIMIT_EXCEED_403.name());
        }

        @Test
        @DisplayName("요청한 eventId가 repository에 없는 경우, NO_SUCH_EVENT_FOUND_400 예외 발생")
        void noSuchFightEventId_thenThrow_NO_SUCH_EVENT_FOUND_400() {
            when(userRepository.findByEmailWithLock(user.getEmail())).thenReturn(user);
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(currentEventDto);
            when(betCardRepository.countByUserIdAndFightEventId(user.getId(), eventId)).thenReturn(0);
            when(fightEventRepository.findByIdWithFfes(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> betService.predict(user.getEmail(), betRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(NO_SUCH_EVENT_FOUND_400.name());
        }

        @Test
        @DisplayName("배팅 항목에 포함된 경기가 하나라도 cancel 상태인 경우, FIGHT_CANCELED_400 예외 발생")
        void fightCanceled_thenThrow_FIGHT_CANCELED_400() {
            int rangeEndMinusOne = FFE_END_RANGE - 1;
            List<FighterFightEvent> ffesWithCanceled = new ArrayList<>(IntStream.range(0, rangeEndMinusOne)
                    .mapToObj(i -> FighterFightEventFixture.builder((long) i, (long) i, (long) (i + 10))
                            .fightEvent(fightEvent).build()).toList());
            ffesWithCanceled.add(FighterFightEventFixture.builder((long) rangeEndMinusOne, (long) rangeEndMinusOne,
                            (long) rangeEndMinusOne + 10).fightEvent(fightEvent)
                    .canceled().build());
            List<SingleBetCardRequest> betCardsRequestWithCanceledFight = LongStream.range(0, FFE_END_RANGE)
                    .mapToObj(i -> new SingleBetCardRequest(i, BetPrediction.builder()
                            .myWinnerId(i)
                            .myLoserId(i + 10)
                            .build())).toList();
            SingleBetRequest wrongBetRequest = new SingleBetRequest(eventId, seedPoint, betCardsRequestWithCanceledFight);

            when(userRepository.findByEmailWithLock(user.getEmail())).thenReturn(user);
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(currentEventDto);
            when(betCardRepository.countByUserIdAndFightEventId(user.getId(), eventId)).thenReturn(0);
            when(fightEventRepository.findByIdWithFfes(eventId)).thenReturn(Optional.ofNullable(fightEvent));
            for (int i = 0; i < FFE_END_RANGE; i++)
                when(fighterFightEventRepository.findById((long) i)).thenReturn(Optional.ofNullable(ffesWithCanceled.get(i)));

            assertThatThrownBy(() -> betService.predict(user.getEmail(), wrongBetRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(FIGHT_CANCELED_400.name());
        }

        private static Stream<LocalDateTime> pastEventStartDateTimes() {
            return Stream.of(
                    LocalDateTime.now().minusSeconds(10),
                    LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().minusDays(1)
            );
        }

        static IntStream existingBetCounts() {
            return IntStream.range(0, BET_AVAILABLE_COUNT);
        }
    }

    @Nested
    @DisplayName("BetService.deleteBet() test")
    class DeleteBetTest {

        private final int seedPoint = 300;
        private final int userPoint = 500;
        private final User user = UserFixture.builder().point(userPoint).build();
        private final Long eventId = 1L;
        private final Long betId = 1L;
        private final FightEvent fightEvent = FightEventFixture.createUpcomingFightEventWithId(eventId);
        private final CurrentEventDto currentEventDto = toDto(fightEvent);
        private final List<FighterFightEvent> ffes = IntStream.range(0, 6)
                .mapToObj(i -> FighterFightEventFixture.builder((long) i, (long) i, (long) (i + 10))
                        .fightEvent(fightEvent).build()).toList();
        private final List<BetCard> betCards = IntStream.range(0, 3)
                .mapToObj(i -> BetCardFixture.builder(ffes.get(i))
                        .winner((long) i, (long) (i + 10))
                        .winMethod(WinMethod.KO_TKO)
                        .build())
                .toList();
        private final Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, betCards);

        private int prevPoint;

        @BeforeEach
        void setup() {
            prevPoint = user.getPoint();
        }

        @ParameterizedTest
        @MethodSource("cancelCounts")
        @DisplayName("배팅 취소 성공 시, 사용자 포인트 복구")
        void deleteBetSuccess_thenReturnRestoredUserPoint(long cancelCount) {
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(currentEventDto);
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
            when(betCancelCountRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment(anyString())).thenReturn(cancelCount);
            when(betRepository.findByFightEventIdAndUserId(eventId, user.getId())).thenReturn(List.of());

            try (MockedStatic<TransactionSynchronizationManager> ignored =
                         Mockito.mockStatic(TransactionSynchronizationManager.class)) {
                BetDeleteResponse response = betService.deletePrediction(user.getEmail(), betId);

                assertThat(response.userPoint()).isEqualTo(prevPoint + seedPoint);
                assertThat(user.getPoint()).isEqualTo(prevPoint + seedPoint);
            }
        }

        @ParameterizedTest
        @MethodSource("pastEventStartDateTimes")
        @DisplayName("이벤트 시작 날짜가 이미 지난 경우, BET_NOT_AVAILABLE_DATE_403 예외 발생")
        void deleteBet_eventAlreadyStarted_thenThrow_BET_NOT_AVAILABLE_DATE_403(LocalDateTime startedAt) {
            FightEvent startedEvent = FightEventFixture.builder(eventId).earlyDT(startedAt).build();
            CurrentEventDto startedEventDto = toDto(startedEvent);

            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(startedEventDto);

            assertThatThrownBy(() -> betService.deletePrediction(user.getEmail(), betId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(BET_NOT_AVAILABLE_DATE_403.name());
            assertThat(user.getPoint()).isEqualTo(prevPoint);
        }

        @Test
        @DisplayName("betId가 repository에 없는 경우, RESOURCE_NOT_FOUND 예외 발생")
        void noSuchBetId_thenThrow_RESOURCE_NOT_FOUND() {
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(currentEventDto);
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(betRepository.findById(betId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> betService.deletePrediction(user.getEmail(), betId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(RESOURCE_NOT_FOUND.name());
            assertThat(user.getPoint()).isEqualTo(prevPoint);
        }

        @Test
        @DisplayName("배팅 취소 한도 초과 시, BET_LIMIT_EXCEED_403 예외 발생")
        void exceedCancelLimit_thenThrow_BET_LIMIT_EXCEED_403() {
            when(currentEventRedisUtils.getData(CURRENT_EVENT.getKey())).thenReturn(currentEventDto);
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
            when(betCancelCountRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment(anyString())).thenReturn((long) BET_CANCEL_AVAILABLE_COUNT + 1);

            assertThatThrownBy(() -> betService.deletePrediction(user.getEmail(), betId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(BET_CANCEL_LIMIT_EXCEED_403.name());
            assertThat(user.getPoint()).isEqualTo(prevPoint);
        }

        private static Stream<LocalDateTime> pastEventStartDateTimes() {
            return Stream.of(
                    LocalDateTime.now().minusSeconds(10),
                    LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().minusDays(1)
            );
        }

        static LongStream cancelCounts() {
            return LongStream.rangeClosed(1, BET_CANCEL_AVAILABLE_COUNT);
        }
    }

}
