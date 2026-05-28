package my.mma.api.fightevent.utils;

import my.mma.api.fightevent.dto.CardStartDateTimeInfoDto;
import my.mma.api.fightevent.entity.property.CardStartDateTimeInfo;

import java.time.LocalDate;

public class FightEventUtils {

    public static LocalDate resolveDisplayDateFromDtos(CardStartDateTimeInfoDto... infos) {
        for (CardStartDateTimeInfoDto info : infos) {
            if (info != null && info.date() != null) {
                return info.date();
            }
        }
        return null;
    }

    public static LocalDate resolveDisplayDate(CardStartDateTimeInfo... infos) {
        for (CardStartDateTimeInfo info : infos) {
            if (info != null && info.getDate() != null) {
                return info.getDate();
            }
        }
        return null;
    }

}
