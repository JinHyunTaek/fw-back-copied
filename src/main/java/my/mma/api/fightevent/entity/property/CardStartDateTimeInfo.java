package my.mma.api.fightevent.entity.property;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Setter
@Embeddable
@Builder
@AllArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PROTECTED)
public class CardStartDateTimeInfo {

    private LocalDate date;

    private LocalTime time;

}
