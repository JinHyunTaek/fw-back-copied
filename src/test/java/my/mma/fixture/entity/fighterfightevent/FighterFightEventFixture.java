package my.mma.fixture.entity.fighterfightevent;

import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.FightResult;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fightevent.entity.property.WinMethod;
import my.mma.fixture.entity.fighter.FighterFixture;

public class FighterFightEventFixture {

    public static FighterFightEventBuilder builder(Long pk, Long winnerId, Long loserId) {
        return new FighterFightEventBuilder(pk, winnerId, loserId);
    }

    public static FighterFightEventBuilder builder(Long winnerId, Long loserId) {
        return new FighterFightEventBuilder(winnerId, loserId);
    }

    public static FighterFightEvent createForPersist(FightEvent fightEvent, Fighter winner, Fighter loser) {
        return FighterFightEvent.builder()
                .fightEvent(fightEvent)
                .winner(winner)
                .loser(loser)
                .fightResult(FightResult.builder()
                        .nc(false)
                        .draw(false)
                        .round(3)
                        .winMethod(WinMethod.U_DEC)
                        .build())
                .title(false)
                .fightWeight(FightWeight.LIGHTWEIGHT)
                .build();
    }

    // FightEventFixture에서 사용 중 - 유지
    public static FighterFightEvent upcomingWithId(FightEvent fightEvent, int id) {
        return FighterFightEvent.builder()
                .id((long) id)
                .fightEvent(fightEvent)
                .winner(FighterFixture.createFighter())
                .loser(FighterFixture.createFighter())
                .title(false)
                .fightResult(null)
                .fightWeight(FightWeight.LIGHTWEIGHT)
                .build();
    }

    public static class FighterFightEventBuilder {
        private long pk = 1L;
        private final Long winnerId;
        private final Long loserId;
        private WinMethod winMethod = WinMethod.DEC;
        private Integer round;
        private boolean isFotN;
        private boolean isPotN;
        private boolean draw;
        private boolean canceled;
        private boolean isNc;
        private FightEvent fightEvent;

        private FighterFightEventBuilder(long pk, Long winnerId, Long loserId) {
            this.winnerId = winnerId;
            this.loserId = loserId;
        }

        private FighterFightEventBuilder(Long winnerId, Long loserId) {
            this.winnerId = winnerId;
            this.loserId = loserId;
        }

        public FighterFightEventBuilder winMethod(WinMethod winMethod) {
            this.winMethod = winMethod;
            return this;
        }

        public FighterFightEventBuilder fightEvent(FightEvent fightEvent) {
            this.fightEvent = fightEvent;
            return this;
        }

        public FighterFightEventBuilder round(int round) {
            this.round = round;
            return this;
        }

        public FighterFightEventBuilder fotN() {
            this.isFotN = true;
            return this;
        }

        public FighterFightEventBuilder potN() {
            this.isPotN = true;
            return this;
        }

        public FighterFightEventBuilder draw() {
            this.draw = true;
            return this;
        }

        public FighterFightEventBuilder canceled() {
            this.canceled = true;
            return this;
        }

        public FighterFightEventBuilder nc() {
            this.isNc = true;
            return this;
        }

        public FighterFightEvent build() {
            FightResult fightResult = draw
                    ? FightResult.builder().draw(true).build()
                    : FightResult.builder()
                    .winMethod(winMethod)
                    .nc(isNc)
                    .round(round != null ? round : 0)
                    .build();

            return FighterFightEvent.builder()
                    .id(pk)
                    .winner(FighterFixture.createFighterWithId(winnerId))
                    .loser(FighterFixture.createFighterWithId(loserId))
                    .fotN(isFotN)
                    .potN(isPotN)
                    .canceled(canceled)
                    .title(false)
                    .fightEvent(fightEvent)
                    .fightResult(fightResult)
                    .build();
        }
    }
}
