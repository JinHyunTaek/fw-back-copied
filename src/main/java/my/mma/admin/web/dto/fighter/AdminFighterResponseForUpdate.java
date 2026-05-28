package my.mma.admin.web.dto.fighter;

import lombok.Builder;
import my.mma.api.fighter.entity.Country;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;

import java.time.LocalDate;

@Builder
public record AdminFighterResponseForUpdate(Long id, String name, String koreanName, String nickname, Integer ranking, FightRecord record,
                                            Integer reach, Double weight, Integer height, LocalDate birthday, Country nationality) {

    public static AdminFighterResponseForUpdate toDto(Fighter fighter){
        return AdminFighterResponseForUpdate.builder()
                .id(fighter.getId())
                .name(fighter.getName())
                .koreanName(fighter.getKoreanName())
                .nickname(fighter.getNickname())
                .reach(fighter.getReach())
                .height(fighter.getHeight())
                .weight(fighter.getWeight())
                .birthday(fighter.getBirthday())
                .record(fighter.getFightRecord())
                .ranking(fighter.getRanking())
                .nationality(fighter.getNationality())
                .build();
    }

}
