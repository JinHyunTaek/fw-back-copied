package my.mma.admin.event.service;

import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.repository.BetRepository;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.WinMethod;
import my.mma.api.global.fcm.AdminPushNotificationService;
import my.mma.api.user.entity.User;
import my.mma.fixture.entity.bet.BetCardFixture;
import my.mma.fixture.entity.bet.BetFixture;
import my.mma.fixture.entity.fighterfightevent.FighterFightEventFixture;
import my.mma.fixture.entity.user.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static my.mma.api.bet.constant.PredictionPolicy.*;
import static my.mma.api.fightevent.entity.property.WinMethod.DEC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
public class BetPointHandlerTest {

    @InjectMocks
    private BetPointHandler betPointHandler;

    @Mock
    private BetRepository betRepository;

    @Mock
    private AdminPushNotificationService adminPushNotificationService;

    private User user;
    int prevEarnedBetSucceedPoint;
    int prevPoint;
    private final int seedPoint = 300;
    private final Long eventId = 1L;
    private final Long FIGHTER_ID_1 = 1L;
    private final Long FIGHTER_ID_2 = 2L;
    private final Long FIGHTER_ID_3 = 3L;
    private final Long FIGHTER_ID_4 = 4L;

    @BeforeEach
    void setupUser() {
        user = UserFixture.builder().build();
        prevEarnedBetSucceedPoint = user.getEarnedBetSucceedPoint();
        prevPoint = user.getPoint();
    }

    @Nested
    @DisplayName("단일 배팅")
    class SinglePrediction {

        @Nested
        @DisplayName("단일 배팅 성공 케이스")
        class SinglePredictionSuccessCase {

            @Test
            @DisplayName("승자 이름 항목만 배팅 시 맞췄을 때 점수 300 지급")
            void winnerNameCorrect_ReturnsTwoTimeMultiplePoints() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint());
            }

            @Test
            @DisplayName("무승부 배팅 맞췄을 때 점수 3000 지급")
            void drawCorrect_Returns15TimesPoints() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).draw().build();
                BetCard betCard = BetCardFixture.builder(ffe).draw().build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, DRAW_HIT.getPoint());
            }

            @Test
            @DisplayName("무승부, 파이트 오브 더 나잇 배팅 맞췄을 때 점수 3200 지급")
            void drawAndFotNCorrect_Returns30TimesPoints() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).draw().fotN().build();
                BetCard betCard = BetCardFixture.builder(ffe).draw().fotN().build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, DRAW_HIT.getPoint() + OTN_HIT.getPoint());
            }

            @Test
            @DisplayName("무승부, 파이트 오브 더 나잇, 퍼포먼스 오브 더 나잇 배팅 맞췄을 때 점수 3400 지급")
            void drawAndFotNPotNCorrect_Returns60TimesPoints() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).draw().fotN().potN().build();
                BetCard betCard = BetCardFixture.builder(ffe).draw().fotN().potN().build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, DRAW_HIT.getPoint() + OTN_HIT.getPoint() * 2);
            }

            @Test
            @DisplayName("승자 맞췄지만 경기 결과 DQ인 경우 점수 300 지급")
            void winnerNameCorrectAndFightResultDQ_ReturnsTwoTimesMultiplePoints() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.DQ).build();
                BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"U_DEC", "M_DEC", "S_DEC"})
            @DisplayName("승자 맞추고 승리 방식이 판정승(U_DEC / M_DEC / S_DEC)인 것까지 맞춘 경우")
            void winnerNameCorrectAndFightResultCorrectWithDEC(WinMethod fightResultWinMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(fightResultWinMethod).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(DEC)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + WIN_METHOD_DEC_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"KO_TKO", "SUB"})
            @DisplayName("승자 맞추고 승리 방식이 KO_TKO/서브미션인 것까지 맞춘 경우")
            void winnerNameCorrectAndFightResultCorrectWithKO_TKO_OR_SUBMISSION(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethod)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint());
            }

            @Test
            @DisplayName("승자, 라운드 맞춘 경우")
            void winnerNameCorrectAndRoundCorrect() {
                int round = 3;
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).round(round).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .finishRound(round)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + FINISH_ROUND_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"KO_TKO", "SUB"})
            @DisplayName("승자, KO_TKO/서브미션 승리 방식, 라운드까지 모두 맞춘 경우")
            void winnerNameCorrectAndKO_TKO_OR_SUBWinMethodCorrectAndRoundCorrect(WinMethod fightResultWinMethod) {
                int round = 3;
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(fightResultWinMethod).round(round).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(fightResultWinMethod)
                        .finishRound(round)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint() + FINISH_ROUND_HIT.getPoint());
            }

            @Test
            @DisplayName("승자, 파이트 오브 더 나잇 맞춘 경우")
            void winnerNameCorrectAndFotNCorrect() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).fotN().build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + OTN_HIT.getPoint());
            }

            @Test
            @DisplayName("승자, 퍼포먼스 오브 더 나잇 맞춘 경우")
            void winnerNameCorrectAndPotNCorrect() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).potN().build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .potN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + OTN_HIT.getPoint());
            }

            @Test
            @DisplayName("승자, 퍼포먼스 오브 더 나잇, 파이트 오브 더 나잇 모두 맞춘 경우")
            void winnerNameCorrectAndPotNAndFotNCorrect() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).fotN().potN().build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .fotN()
                        .potN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + OTN_HIT.getPoint() * 2);
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"KO_TKO", "SUB"})
            @DisplayName("승자, 파이트 오브 더 나잇, KO_TKO/SUB 맞춘 경우")
            void winnerNameCorrectAndFotNAndKO_TKO_OR_SUBCorrect(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).fotN().build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethod)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + OTN_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"M_DEC", "S_DEC", "U_DEC"})
            @DisplayName("승자, 파이트 오브 더 나잇, 판정승 맞춘 경우")
            void winnerNameCorrectAndFotNAndDECCorrect(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).fotN().build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(DEC)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + OTN_HIT.getPoint() + WIN_METHOD_DEC_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"KO_TKO", "SUB"})
            @DisplayName("승자, 파이트 오브 더 나잇, KO_TKO/SUB, 라운드 맞춘 경우, 풀옵션 보너스 점수까지 지급")
            void winnerNameAndFotNAndKO_TKO_OR_SUB_AND_RoundCorrect_ReturnsAdditionalFullOptionBonus(WinMethod winMethod) {
                int round = 3;
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).fotN().round(round).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethod)
                        .finishRound(round)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + OTN_HIT.getPoint()
                        + WIN_METHOD_FINISH_HIT.getPoint() + FINISH_ROUND_HIT.getPoint() + FULL_OPTION_BONUS.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"S_DEC", "U_DEC", "M_DEC"})
            @DisplayName("승자, 파이트 오브 더 나잇, 판정승 맞춘 경우")
            void winnerNameAndFotNAndDecAndRoundCorrect(WinMethod winMethod) {
                int round = 3;
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).fotN().build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(DEC)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + OTN_HIT.getPoint()
                        + WIN_METHOD_DEC_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"M_DEC", "S_DEC", "U_DEC", "SUB", "KO_TKO"})
            @DisplayName("승자 맞췄지만 승리 방식 배팅하지 않은 경우")
            void winnerNameCorrectAndDoesntPickWinMethod(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).build();
                BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"M_DEC", "S_DEC", "U_DEC"})
            @DisplayName("승자, 승리 방식(판정승) 맞췄지만 라운드 배팅하지 않은 경우")
            void winnerNameWinMethodDECCorrectAndDoesntPickRound(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).round(2).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(DEC)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + WIN_METHOD_DEC_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"SUB", "KO_TKO"})
            @DisplayName("승자, 승리 방식(SUB/KO_TKO) 맞췄지만 라운드 배팅하지 않은 경우")
            void winnerNameWinMethodKO_TKO_OR_SUBCorrectAndDoesntPickRound(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).round(2).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethod)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"SUB", "KO_TKO"})
            @DisplayName("승자, 승리 방식(SUB/KO_TKO), FOTN 맞췄지만 라운드 배팅하지 않은 경우")
            void winnerNameWinMethodKO_TKO_OR_SUBFotNCorrectAndDoesntPickRound(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).round(2).fotN().build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethod)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet, WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint() + OTN_HIT.getPoint());
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"SUB", "KO_TKO"})
            @DisplayName("승자, 승리 방식(SUB/KO_TKO), 라운드 맞췄지만 FOTN 배팅하지 않은 경우")
            void winnerNameWinMethodKO_TKO_OR_SUBRoundCorrectAndDoesntPickFOTN(WinMethod winMethod) {
                int winRound = 2;
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).round(winRound).fotN().build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethod)
                        .finishRound(winRound)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertSucceeded(bet,
                        WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint() + FINISH_ROUND_HIT.getPoint());
            }
        }

        @Nested
        @DisplayName("단일 배팅 실패 케이스")
        class SinglePredictionFailCase {

            @Test
            @DisplayName("승자 이름 항목만 배팅 시 틀렸을 때 포인트 지급하지 않음")
            void winnerNameIncorrect_DoesntReturnPoint() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_2, FIGHTER_ID_1).build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @Test
            @DisplayName("무승부 배팅 틀렸을 때 포인트 지급하지 않음(경기 결과 무승부, 사용자는 승자, 패자 픽)")
            void drawInCorrect_DoesntReturnPoint1() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).draw().build();
                BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"SUB", "KO_TKO", "M_DEC", "S_DEC", "U_DEC"})
            @DisplayName("무승부 배팅 틀렸을 때 포인트 지급하지 않음(사용자 무승부 픽, 경기 결과는 승자, 패자가 있는 경기)")
            void drawInCorrect_DoesntReturnPoint2(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).build();
                BetCard betCard = BetCardFixture.builder(ffe).draw().build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @Test
            @DisplayName("승자 못 맞추고 경기 결과 DQ인 경우 포인트 지급하지 않음")
            void winnerNameInCorrectAndFightResultDQ_DoesntReturnPoint() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.DQ).build();
                BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_2, FIGHTER_ID_1).build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"KO_TKO", "SUB"})
            @DisplayName("승자 맞췄지만 경기 결과가 판정승인 승리 방식을 KO/서브미션으로 예측하여 틀렸을 때 포인트 지급하지 않음")
            void winnerNameCorrectAndFightResultIncorrectWithDEC_DoesntReturnPoint(WinMethod userIncorrectWinMethod) {
                List<WinMethod> fightResultDecList = new ArrayList<>(List.of(WinMethod.U_DEC, WinMethod.M_DEC, WinMethod.S_DEC));
                for (WinMethod fightResultDec : fightResultDecList) {
                    user.updateEarnedBetSucceedPoint(prevEarnedBetSucceedPoint);
                    FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(fightResultDec).build();
                    BetCard betCard = BetCardFixture.builder(ffe)
                            .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                            .winMethod(userIncorrectWinMethod)
                            .build();
                    Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                    handleBets(bet);

                    assertFailed(bet);
                }
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"KO_TKO", "SUB"})
            @DisplayName("승자 맞췄지만 경기 결과가 KO_TKO/서브미션인 승리 방식을 판정승으로 예측하여 틀렸을 때 포인트 지급하지 않음")
            void winnerNameCorrectAndFightResultIncorrectWithKO_TKO_OR_SUBMISSION_DoesntReturnPoint(WinMethod fightResultWinMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(fightResultWinMethod).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(DEC)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"U_DEC", "M_DEC", "S_DEC"})
            @DisplayName("승자, (판정승)승리 방식 모두 맞췄지만 라운드 맞추지 못한 경우 포인트 지급하지 않음")
            void winnerNameCorrectAndWinMethodDECCorrectAndRoundIncorrect_DoesntReturnPoint(WinMethod winMethodDEC) {
                int fightResultRound = 3;
                int pickRound = 1;
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethodDEC).round(fightResultRound).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethodDEC)
                        .finishRound(pickRound)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"KO_TKO", "SUB"})
            @DisplayName("승자, (KO_TKO/SUB)승리 방식 모두 맞췄지만 라운드 맞추지 못한 경우 포인트 지급하지 않음")
            void winnerNameCorrectAndWinMethodKO_TKO_OR_SUBCorrectAndRoundIncorrect_DoesntReturnPoint(WinMethod winMethodKO_TKO_SUB) {
                int fightResultRound = 3;
                int pickRound = 1;
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethodKO_TKO_SUB).round(fightResultRound).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethodKO_TKO_SUB)
                        .finishRound(pickRound)
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @Test
            @DisplayName("승자 맞췄지만 해당 경기가 FOTN이 아닌데, FOTN 픽한 경우, 포인트 미지급")
            void winnerNameCorrectAndFotNIncorrectReturnZeroPoint() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(DEC)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @Test
            @DisplayName("승자 맞췄지만 해당 경기가 POTN이 아닌데, POTN 픽한 경우, 포인트 미지급")
            void winnerNameCorrectAndPotNIncorrectReturnZeroPoint() {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(DEC)
                        .potN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @ParameterizedTest
            @DisplayName("승자, 승리 방식(KO_TKO/SUB) 맞췄지만 해당 경기가 FOTN이 아닌데, FOTN 픽한 경우, 포인트 미지급")
            @EnumSource(value = WinMethod.class, names = {"SUB", "KO_TKO"})
            void winnerNameAndWinMethodKO_TKO_SUB_CorrectAndFotNIncorrectReturnZeroPoint(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(winMethod)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }

            @ParameterizedTest
            @DisplayName("승자, 판정승 맞췄지만 해당 경기가 FOTN이 아닌데, FOTN 픽한 경우, 포인트 미지급")
            @EnumSource(value = WinMethod.class, names = {"U_DEC", "M_DEC", "S_DEC"})
            void winnerNameAndWinMethodDECCorrectAndFotNIncorrectReturnZeroPoint(WinMethod winMethod) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethod).build();
                BetCard betCard = BetCardFixture.builder(ffe)
                        .winner(FIGHTER_ID_1, FIGHTER_ID_2)
                        .winMethod(DEC)
                        .fotN()
                        .build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);

                handleBets(bet);

                assertFailed(bet);
            }
        }
    }

    @Nested
    @DisplayName("조합 배팅")
    class MultiplePrediction {

        @Nested
        @DisplayName("조합 배팅 성공 케이스")
        class MultiplePredictionSuccessCase {

            @Test
            @DisplayName("승자 이름 배팅 2회 맞췄을 때")
            void winnerNameCorrect2Times() {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_3, FIGHTER_ID_4).build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertSucceeded(bet, (WINNER_HIT.getPoint() + COMBO_BONUS_PER_OPTION.getPoint()) * 2);
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"U_DEC", "M_DEC", "S_DEC"})
            @DisplayName("[승자 이름] 배팅, [승자 이름, 승리 방식(판정승)] 배팅카드 총 2개 맞췄을 때")
            void winnerNameAndWinnerNameWinMethodDECCorrect(WinMethod winMethod) {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.KO_TKO).round(2).build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(winMethod).build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(DEC).build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertSucceeded(bet,
                        WINNER_HIT.getPoint() +
                                WINNER_HIT.getPoint() + WIN_METHOD_DEC_HIT.getPoint() +
                                COMBO_BONUS_PER_OPTION.getPoint() * 3
                );
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"SUB", "KO_TKO"})
            @DisplayName("[승자 이름, 승리 방식(SUB/KO_TKO)] 배팅], [승자 이름, 승리 방식(판정승)] 배팅카드 총 2개 맞췄을 때")
            void winnerNameWinMethodSUB_OR_KO_TKOAndWinnerNameWinMethodDECCorrect(WinMethod nonDecwinMethod) {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(nonDecwinMethod).round(2).build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(WinMethod.M_DEC).build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(nonDecwinMethod).build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(DEC).build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertSucceeded(bet,
                        WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint() +
                                WINNER_HIT.getPoint() + WIN_METHOD_DEC_HIT.getPoint() +
                                COMBO_BONUS_PER_OPTION.getPoint() * 4
                );
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"SUB", "KO_TKO"})
            @DisplayName("[승자 이름, 승리 방식(SUB/KO_TKO)] 배팅, [승자 이름, 승리 방식(판정승), 파이트 오브 더 나잇] 배팅카드 총 2개 맞췄을 때")
            void winnerNameWinMethodSUB_OR_KO_TKOAndWinnerNameWinMethodDECFotNCorrect_Returns24TimesMultiplePoints(WinMethod nonDecwinMethod) {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(nonDecwinMethod).round(2).build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(WinMethod.M_DEC).fotN().build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(nonDecwinMethod).build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(DEC).fotN().build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertSucceeded(bet,
                        WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint() +
                                WINNER_HIT.getPoint() + WIN_METHOD_DEC_HIT.getPoint() + OTN_HIT.getPoint() +
                                COMBO_BONUS_PER_OPTION.getPoint() * 5
                );
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"M_DEC", "U_DEC", "S_DEC"})
            @DisplayName("무승부 배팅, [승자 이름, 승리 방식(판정승), 파이트 오브 더 나잇] 배팅카드 총 2개 맞췄을 때")
            void drawAndWinnerNameWinMethodDECFotNCorrect(WinMethod dec) {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).draw().build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(dec).fotN().build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).draw().build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(DEC).fotN().build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertSucceeded(bet,
                        DRAW_HIT.getPoint() +
                                WINNER_HIT.getPoint() + WIN_METHOD_DEC_HIT.getPoint() + OTN_HIT.getPoint() +
                                COMBO_BONUS_PER_OPTION.getPoint() * 4
                );
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"SUB", "KO_TKO"})
            @DisplayName("무승부 배팅, [승자 이름, 승리 방식(SUB/KO_TKO), 파이트 오브 더 나잇] 배팅카드 총 2개 맞췄을 때")
            void drawAndWinnerNameWinMethodDECFotNCorrect_Returns120TimesMultiplePoints(WinMethod nonDec) {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).draw().build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(nonDec).fotN().build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).draw().build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(nonDec).fotN().build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertSucceeded(bet,
                        DRAW_HIT.getPoint() +
                                WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint() + OTN_HIT.getPoint() +
                                COMBO_BONUS_PER_OPTION.getPoint() * 4
                );
            }
        }

        @Nested
        @DisplayName("조합 배팅 실패 케이스")
        class MultiplePredictionFailCase {

            @Test
            @DisplayName("승자 이름 배팅 1회 맞추고 1회 틀렸을 때 포인트 미지급")
            void winnerNameCorrectAndWinnerNameIncorrect_DoesntReturnPoint() {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.KO_TKO).build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_4, FIGHTER_ID_3).build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertFailed(bet);
            }

            @Test
            @DisplayName("승자 이름 배팅 2회 맞추고 1회 틀렸을 때 포인트 미지급")
            void winnerNameCorrectX2AndWinnerNameIncorrect_DoesntReturnPoint() {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.KO_TKO).build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.KO_TKO).build();
                FighterFightEvent ffe3 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                BetCard betCard3 = BetCardFixture.builder(ffe3).winner(FIGHTER_ID_4, FIGHTER_ID_3).build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2, betCard3)));

                handleBets(bet);

                assertFailed(bet);
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"U_DEC", "S_DEC", "M_DEC"})
            @DisplayName("승자 이름 배팅, 승리 방식(DEC) 1회 맞추고 1회 틀렸을 때 포인트 미지급")
            void winnerNameWinMethodDECCorrectAndIncorrect_DoesntReturnPoint(WinMethod winMethodDEC) {
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethodDEC).build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(winMethodDEC).build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethodDEC).build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_4, FIGHTER_ID_3).winMethod(WinMethod.KO_TKO).build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertFailed(bet);
            }

            @ParameterizedTest
            @EnumSource(value = WinMethod.class, names = {"U_DEC", "S_DEC", "M_DEC"})
            @DisplayName("승자 이름 배팅, 승리 방식(DEC), 라운드 1회 맞추고 나머지 1회 틀렸을 때 포인트 미지급")
            void winnerNameWinMethodDECRoundCorrectAndIncorrect_DoesntReturnPoint(WinMethod winMethodDEC) {
                int correctRound = 1;
                int incorrectRound = 3;
                FighterFightEvent ffe1 = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethodDEC).build();
                FighterFightEvent ffe2 = FighterFightEventFixture.builder(FIGHTER_ID_3, FIGHTER_ID_4).winMethod(winMethodDEC).round(correctRound).build();
                BetCard betCard1 = BetCardFixture.builder(ffe1).winner(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(winMethodDEC).build();
                BetCard betCard2 = BetCardFixture.builder(ffe2).winner(FIGHTER_ID_4, FIGHTER_ID_3).winMethod(winMethodDEC).finishRound(incorrectRound).build();
                Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, new ArrayList<>(List.of(betCard1, betCard2)));

                handleBets(bet);

                assertFailed(bet);
            }
        }
    }

    @Nested
    @DisplayName("환불 가능 케이스(Some Fights Cancled/NC)")
    class Refundable {

        @Test
        @DisplayName("배팅했던 모든 경기가 취소된 경우(betList, single bet with single bet card), 배팅 시 사용했던 모든 포인트 지급")
        void allFightCanceledThenRefundAllPoints1() {
            int seedPointSum = 0;
            List<Bet> bets = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).canceled().build();
                BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);
                seedPointSum += bet.getSeedPoint();
                bets.add(bet);
            }

            handleBets(bets);

            assertThat(user.getPoint()).isEqualTo(prevPoint + seedPointSum);
        }

        @Test
        @DisplayName("배팅했던 모든 경기가 취소된 경우(single bet with betCards), 배팅 시 사용했던 모든 포인트 지급")
        void allFightCanceledThenRefundAllPoints2() {
            List<BetCard> betCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).canceled().build();
                betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build());
            }
            Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, betCards);

            handleBets(bet);

            assertThat(user.getPoint()).isEqualTo(prevPoint + bet.getSeedPoint());
        }

        @Test
        @DisplayName("배팅했던 모든 경기가 NC(No Contest)인 경우, 배팅 시 사용했던 모든 포인트 지급" +
                "(betList, single bet with single bet card)")
        void allFightResultNCThenRefundAllPoints1() {
            int seedPointSum = 0;
            List<Bet> bets = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).nc().build();
                BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
                Bet bet = BetFixture.createBet(user, seedPoint, betCard);
                seedPointSum += bet.getSeedPoint();
                bets.add(bet);
            }

            handleBets(bets);

            assertThat(user.getPoint()).isEqualTo(prevPoint + seedPointSum);
        }

        @Test
        @DisplayName("배팅했던 모든 경기가 NC(No Contest)인 경우, 배팅 시 사용했던 모든 포인트 지급" +
                "(single bet with betCards)")
        void allFightResultNCThenRefundAllPoints2() {
            List<BetCard> betCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).nc().build();
                betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build());
            }
            Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, betCards);

            handleBets(bet);

            assertThat(user.getPoint()).isEqualTo(prevPoint + bet.getSeedPoint());
        }

        @Test
        @DisplayName("하나의 경기라도 CANCEL 상태 아닌 경우, 포인트 환불 없음 (나머지 승자, 승리 방식 배팅 성공 케이스)")
        void singleFightIsNotCanceled_ThenNonrefundable1() {
            List<BetCard> betCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).canceled().build();
                betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build());
            }
            FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.KO_TKO).build();
            betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.KO_TKO).build());
            Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, betCards);

            handleBets(bet);

            assertThat(user.getEarnedBetSucceedPoint()).isEqualTo(
                    prevEarnedBetSucceedPoint + WINNER_HIT.getPoint() + WIN_METHOD_FINISH_HIT.getPoint());
            assertThat(bet.getSucceed()).isTrue();
        }

        @Test
        @DisplayName("하나의 경기라도 CANCEL 상태 아닌 경우, 포인트 환불 없음 (나머지 승자, 승리 방식 배팅 실패 케이스)")
        void singleFightIsNotCanceled_ThenNonrefundable2() {
            List<BetCard> betCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).canceled().build();
                betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build());
            }
            FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.KO_TKO).build();
            betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(DEC).build());
            Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, betCards);

            handleBets(bet);

            assertThat(user.getPoint()).isEqualTo(prevPoint);
            assertThat(user.getEarnedBetSucceedPoint()).isEqualTo(prevEarnedBetSucceedPoint);
            assertThat(bet.getSucceed()).isFalse();
        }

        @Test
        @DisplayName("하나의 경기라도 NC가 아닌 경우, 포인트 환불 없음 (승자, 승리 방식 배팅 성공 케이스)")
        void singleFightResultIsNotNC_ThenNonrefundable1() {
            List<BetCard> betCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).nc().build();
                betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build());
            }
            FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.U_DEC).build();
            betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(DEC).build());
            Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, betCards);

            handleBets(bet);

            assertThat(user.getPoint()).isEqualTo(prevPoint);
            assertThat(user.getEarnedBetSucceedPoint()).isEqualTo(
                    prevEarnedBetSucceedPoint + WINNER_HIT.getPoint() + WIN_METHOD_DEC_HIT.getPoint());
            assertThat(bet.getSucceed()).isTrue();
        }

        @Test
        @DisplayName("하나의 경기라도 NC가 아닌 경우, 포인트 환불 없음 (승자, 승리 방식 배팅 실패 케이스)")
        void singleFightResultIsNotNC_ThenNonrefundable2() {
            List<BetCard> betCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).nc().build();
                betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build());
            }
            FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.U_DEC).build();
            betCards.add(BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(WinMethod.KO_TKO).build());
            Bet bet = BetFixture.createBetWithBetCardCollection(user, seedPoint, betCards);

            handleBets(bet);

            assertThat(user.getPoint()).isEqualTo(prevPoint);
            assertThat(user.getEarnedBetSucceedPoint()).isEqualTo(prevEarnedBetSucceedPoint);
            assertThat(bet.getSucceed()).isFalse();
        }
    }

    @DisplayName("배팅 자체에서 의도치 않은 예외" +
            "(ex) ffe nonCanceled & nonNC & ffe winMethod is null)가 발생한 경우, 해당 배팅은 정상 실패 상태로 업데이트" +
            "+ 관리자에게 푸시 알림")
    @Test
    void betSettledIsFalse_whenUnknownExceptionOccurred() {
        FighterFightEvent ffe = FighterFightEventFixture.builder(FIGHTER_ID_1, FIGHTER_ID_2).winMethod(null).build();
        BetCard betCard = BetCardFixture.builder(ffe).winner(FIGHTER_ID_1, FIGHTER_ID_2).build();
        Bet bet = BetFixture.createBet(user, seedPoint, betCard);

        handleBets(bet);

        assertThat(bet.getSettled()).isFalse();
        Mockito.verify(adminPushNotificationService).sendNotificationToAdmin(anyString());
    }

    // --- private helpers ---

    private void handleBets(Bet... bets) {
        Mockito.when(betRepository.findByEventIdWithBetCards(eventId)).thenReturn(List.of(bets));
        betPointHandler.handleUserBets(eventId);
    }

    private void handleBets(List<Bet> bets) {
        Mockito.when(betRepository.findByEventIdWithBetCards(eventId)).thenReturn(bets);
        betPointHandler.handleUserBets(eventId);
    }

    private void assertSucceeded(Bet bet, int expectedPoints) {
        assertThat(user.getEarnedBetSucceedPoint()).isEqualTo(prevEarnedBetSucceedPoint + expectedPoints);
        assertThat(bet.getSucceed()).isTrue();
    }

    private void assertFailed(Bet bet) {
        assertThat(user.getEarnedBetSucceedPoint()).isEqualTo(prevEarnedBetSucceedPoint);
        assertThat(bet.getSucceed()).isFalse();
    }
}