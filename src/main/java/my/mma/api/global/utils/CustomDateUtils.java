package my.mma.api.global.utils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class CustomDateUtils {

    public static Duration getFightDuration(long round, String[] timeParts){
        int minute = Integer.parseInt(timeParts[0]);
        int second = Integer.parseInt(timeParts[1]);

        int roundDurationMin = 5;
        Duration fightDuration;

        if (minute == roundDurationMin && second == 0) {
            // 라운드 종료
            fightDuration = Duration.ofMinutes(round * roundDurationMin);
        } else {
            // 라운드 중 종료
            fightDuration =
                    Duration.ofMinutes((round - 1) * roundDurationMin + minute)
                            .plusSeconds(second);
        }
        return fightDuration;
    }

    public static boolean isThisWeekend(LocalDate date) {
        DayOfWeek currentWeek = LocalDate.now().getDayOfWeek();
        return isThisWeek(date) &&
                (currentWeek.equals(DayOfWeek.SATURDAY)
                        || currentWeek.equals(DayOfWeek.SUNDAY));
    }

    public static boolean isThisWeek(LocalDate date) {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek);
    }

}