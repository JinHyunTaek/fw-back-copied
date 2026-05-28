package my.mma.api.fighter.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FightRecord {

    private int win;
    private int draw;
    private int loss;

    public static FightRecord toFightRecord(String[] split_record){
        return FightRecord.builder()
                .win(Integer.parseInt(split_record[0]))
                .loss(Integer.parseInt(split_record[1]))
                .draw(((int) split_record[2].charAt(0)) - 48)
                .build();
    }

}
