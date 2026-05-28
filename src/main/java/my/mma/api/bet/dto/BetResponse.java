package my.mma.api.bet.dto;

import lombok.Builder;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.entity.BetPrediction;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fightevent.entity.FighterFightEvent;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BetResponse(String eventName, List<SingleBetResponse> singleBets) {

    @Builder
    public record SingleBetResponse(long betId, List<SingleBetCardResponse> betCards,
                                    Boolean succeed, LocalDateTime createdDateTime, int seedPoint, boolean isRefunded) {

        public static SingleBetResponse toDto(Bet bet) {
            return SingleBetResponse.builder()
                    .betId(bet.getId())
                    .succeed(bet.getSucceed())
                    .isRefunded(bet.isRefunded())
                    .createdDateTime(bet.getCreatedDateTime())
                    .betCards(bet.getBetCards().stream().map(
                            SingleBetCardResponse::toDto
                    ).toList())
                    .seedPoint(bet.getSeedPoint())
                    .build();
        }

    }

    @Builder
    public record SingleBetCardResponse(
            String redName,
            String blueName,
            String myWinnerName,
            String myLoserName,
            boolean isCanceled,
            boolean isFiveRound,
            BetPrediction betPrediction) {

        public static SingleBetCardResponse toDto(BetCard betCard) {
            FighterFightEvent ffe = betCard.getFighterFightEvent();
            Fighter winner = ffe.getWinner();
            Fighter loser = ffe.getLoser();
            BetPrediction prediction = betCard.getBetPrediction();
            String redName = winner.getKoreanName() != null ? winner.getKoreanName() : winner.getName(); // left
            String blueName = loser.getKoreanName() != null ? loser.getKoreanName() : loser.getName(); // right
            String myWinnerName = null;
            String myLoserName = null;
            if(!prediction.isDraw()) {
                if (winner.getId().equals(prediction.getMyWinnerId())) {
                    myWinnerName = redName;
                    myLoserName = blueName;
                } else {
                    myWinnerName = blueName;
                    myLoserName = redName;
                }
            }
            return SingleBetCardResponse.builder()
                    .redName(redName)
                    .blueName(blueName)
                    .myWinnerName(myWinnerName)
                    .myLoserName(myLoserName)
                    .isCanceled(ffe.isCanceled())
                    .isFiveRound(ffe.isTitle() || ffe.getCardOrder() == 1)
                    .betPrediction(betCard.getBetPrediction())
                    .build();
        }

    }

}
