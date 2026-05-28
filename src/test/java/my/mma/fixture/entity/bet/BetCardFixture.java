package my.mma.fixture.entity.bet;


import my.mma.api.bet.entity.BetCard;
import my.mma.api.bet.entity.BetPrediction;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.WinMethod;

public class BetCardFixture {

    public static BetCardBuilder builder(FighterFightEvent ffe) {
        return new BetCardBuilder(ffe);
    }

    public static class BetCardBuilder {
        private final FighterFightEvent ffe;
        private Long myWinnerId;
        private Long myLoserId;
        private WinMethod winMethod;
        private Integer finishRound;
        private boolean isFotN;
        private boolean isPotN;
        private boolean draw;

        private BetCardBuilder(FighterFightEvent ffe) {
            this.ffe = ffe;
        }

        public BetCardBuilder winner(Long winner, Long loser) {
            this.myWinnerId = winner;
            this.myLoserId = loser;
            return this;
        }

        public BetCardBuilder winMethod(WinMethod winMethod) {
            this.winMethod = winMethod;
            return this;
        }

        public BetCardBuilder finishRound(int round) {
            this.finishRound = round;
            return this;
        }

        public BetCardBuilder fotN() {
            this.isFotN = true;
            return this;
        }

        public BetCardBuilder potN() {
            this.isPotN = true;
            return this;
        }

        public BetCardBuilder draw() {
            this.draw = true;
            return this;
        }

        public BetCard build() {
            return BetCard.builder()
                    .id(1L)
                    .fighterFightEvent(ffe)
                    .betPrediction(BetPrediction.builder()
                            .myWinnerId(myWinnerId)
                            .myLoserId(myLoserId)
                            .winMethod(winMethod)
                            .finishRound(finishRound)
                            .isFotN(isFotN)
                            .isPotN(isPotN)
                            .draw(draw)
                            .build())
                    .build();
        }
    }
}
