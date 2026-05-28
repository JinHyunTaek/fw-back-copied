package my.mma.api.game.dto;

import lombok.Builder;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.WinMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Builder
public record FightGameResponse(
        String eventName,
        String winnerName,
        String loserName,
        SingleFightGameSelection answer,
        List<SingleFightGameSelection> wrongSelections
) {

    public static List<FightGameResponse> of(List<FighterFightEvent> fights) {
        return fights.stream()
                .limit(5)
                .map(ffe -> {
                    String winnerName = ffe.getWinner().getKoreanName() != null ? ffe.getWinner().getKoreanName() : ffe.getWinner().getName();
                    String loserName = ffe.getLoser().getKoreanName() != null ? ffe.getLoser().getKoreanName() : ffe.getLoser().getName();
                    WinMethod winMethod = ffe.getFightResult().getWinMethod();

                    List<SingleFightGameSelection> selections = new ArrayList<>();
                    List<SingleFightGameSelection> allSelections = SingleFightGameSelection.allSelections(winnerName, loserName);
                    Collections.shuffle(allSelections);
                    SingleFightGameSelection answer = SingleFightGameSelection.builder()
                            .name(winnerName)
                            .winMethod(isDec(winMethod) ? WinMethod.DEC : winMethod)
                            .build();
                    for (SingleFightGameSelection allSelection : allSelections) {
                        if (selections.size() == 3)
                            break;
                        if (allSelection.equals(answer))
                            continue;
                        selections.add(allSelection);
                    }

                    return FightGameResponse.builder()
                            .eventName(ffe.getFightEvent().getName())
                            .winnerName(winnerName)
                            .loserName(loserName)
                            .answer(answer)
                            .wrongSelections(selections)
                            .build();
                }).toList();
    }

    private static boolean isDec(WinMethod winMethod) {
        List<WinMethod> decWinMethods = List.of(WinMethod.U_DEC, WinMethod.M_DEC, WinMethod.S_DEC);
        return decWinMethods.contains(winMethod);
    }

    @Builder
    protected record SingleFightGameSelection(
            String name,
            WinMethod winMethod
    ) {
        protected static List<SingleFightGameSelection> allSelections(String winnerName, String loserName) {
            List<WinMethod> winMethods = List.of(WinMethod.KO_TKO, WinMethod.SUB, WinMethod.DEC);
            List<SingleFightGameSelection> selections = new ArrayList<>();
            for (WinMethod winMethod : winMethods) {
                selections.add(SingleFightGameSelection.builder().name(winnerName).winMethod(winMethod).build());
                selections.add(SingleFightGameSelection.builder().name(loserName).winMethod(winMethod).build());
            }
            return selections;
        }
    }

}


