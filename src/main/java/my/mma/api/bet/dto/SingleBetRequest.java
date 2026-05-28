package my.mma.api.bet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.entity.BetPrediction;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.WinMethod;
import my.mma.api.user.entity.User;

import java.util.List;

public record SingleBetRequest(long eventId,
                               @Min(value = 300)
                               @Max(value = 900)
                               int seedPoint,
                               @Size(min = 1, max = 3) List<SingleBetCardRequest> singleBetCards) {

    public Bet toEntity(User user, FightEvent fightEvent) {
        return Bet.builder()
                .seedPoint(seedPoint)
                .fightEvent(fightEvent)
                .user(user)
                .build();
    }

    public record SingleBetCardRequest(long fighterFightEventId, BetPrediction betPrediction) {

        public BetCard toEntity(FighterFightEvent ffe, Bet bet) {
            BetCard betCard = BetCard.builder()
                    .fighterFightEvent(ffe)
                    .betPrediction(betPrediction)
                    .bet(bet)
                    .build();
            if (betPrediction.getWinMethod() != null && betPrediction.getWinMethod().equals(WinMethod.DEC)) {
                betPrediction.updateFromDecRequest();
            }
            if (betPrediction.isDraw()) {
                betPrediction.updateFromDrawRequest();
            }
            return betCard;
        }

    }

}
