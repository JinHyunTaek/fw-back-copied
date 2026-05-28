package my.mma.fixture.entity.bet;

import my.mma.api.bet.entity.Bet;
import my.mma.api.bet.entity.BetCard;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.user.entity.User;

import java.util.ArrayList;
import java.util.List;

public class BetFixture {

    public static Bet createBet(
            User user,
            int seedPoint,
            BetCard...betCards
    ) {
        return Bet.builder()
                .id(1L)
                .seedPoint(seedPoint)
                .user(user)
                .betCards(List.of(betCards))
                .build();
    }

    public static Bet createBetWithBetCardCollection(
            User user,
            int seedPoint,
            List<BetCard> betCards
    ) {
        return Bet.builder()
                .id(1L)
                .seedPoint(seedPoint)
                .user(user)
                .betCards(betCards)
                .build();
    }

}
