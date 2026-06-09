package my.mma.api.game.dto;

import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.FightResult;
import my.mma.api.fightevent.entity.property.WinMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FightGameResponse.of() 변환 로직")
class FightGameResponseTest {

    @ParameterizedTest(name = "winMethod {0} -> answer {1}")
    @CsvSource({
            "U_DEC, DEC",
            "M_DEC, DEC",
            "S_DEC, DEC",
            "KO_TKO, KO_TKO",
            "SUB, SUB"
    })
    @DisplayName("판정승(U/M/S_DEC)은 answer에서 DEC로 통합되고, 나머지는 그대로 유지된다")
    void answerWinMethodNormalizedForDecisions(WinMethod input, WinMethod expected) {
        // given
        String winnerKorName = "정찬성";
        String loserEngName = "Volk";
        FighterFightEvent ffe = ffe("UFC 300",
                fighter("Jung", winnerKorName),
                fighter(loserEngName, null),
                input);

        // when
        FightGameResponse resp = FightGameResponse.of(List.of(ffe)).getFirst();

        // then
        assertThat(resp.answer().winMethod()).isEqualTo(expected);
        assertThat(resp.answer().name()).isEqualTo(winnerKorName);
        assertThat(resp.loserName()).isEqualTo(loserEngName);
    }

    @Test
    @DisplayName("이름은 koreanName이 있으면 그것을, 없으면 영문 name으로 폴백한다")
    void nameFallsBackToEnglishNameWhenKoreanNameNull() {
        // given
        String winnerEngName = "Volkanovski";
        String loserKorName = "정찬성";
        FighterFightEvent ffe = ffe("UFC 300",
                fighter(winnerEngName, null),
                fighter("Jung", loserKorName),
                WinMethod.KO_TKO);

        // when
        FightGameResponse r = FightGameResponse.of(List.of(ffe)).getFirst();

        // then
        assertThat(r.eventName()).isEqualTo("UFC 300");
        assertThat(r.winnerName()).isEqualTo(winnerEngName);
        assertThat(r.loserName()).isEqualTo(loserKorName);
    }

    @Test
    @DisplayName("wrongSelections는 정답을 제외한 후보 중 중복 없는 3개다 (shuffle 무관)")
    void wrongSelectionsAreThreeDistinctNonAnswerCandidates() {
        FighterFightEvent ffe = ffe("UFC 300", fighter("W", null), fighter("L", null), WinMethod.KO_TKO);

        FightGameResponse resp = FightGameResponse.of(List.of(ffe)).getFirst();

        assertThat(resp.wrongSelections()).hasSize(3);
        assertThat(resp.wrongSelections()).doesNotContain(resp.answer());
        assertThat(resp.wrongSelections()).doesNotHaveDuplicates();
        assertThat(resp.wrongSelections()).allSatisfy(sel -> {
            assertThat(sel.name()).isIn("W", "L");
            assertThat(sel.winMethod()).isIn(WinMethod.KO_TKO, WinMethod.SUB, WinMethod.DEC);
        });
    }

    @Test
    @DisplayName("입력이 5개를 초과해도 최대 5문제만 생성한다")
    void limitsToFiveQuestions() {
        List<FighterFightEvent> seven = IntStream.range(0, 7)
                .mapToObj(i -> ffe("UFC " + i, fighter("W" + i, null), fighter("L" + i, null), WinMethod.KO_TKO))
                .toList();

        assertThat(FightGameResponse.of(seven)).hasSize(5);
    }

    private static FighterFightEvent ffe(String eventName, Fighter winner, Fighter loser, WinMethod winMethod) {
        return FighterFightEvent.builder()
                .winner(winner)
                .loser(loser)
                .fightEvent(FightEvent.builder().name(eventName).build())
                .fightResult(FightResult.builder().winMethod(winMethod).build())
                .build();
    }

    private static Fighter fighter(String name, String koreanName) {
        return Fighter.builder().name(name).koreanName(koreanName).build();
    }
}
