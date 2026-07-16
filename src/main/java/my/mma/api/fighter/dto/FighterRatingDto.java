package my.mma.api.fighter.dto;

import lombok.Builder;
import my.mma.api.fighter.entity.Fighter;

@Builder
public record FighterRatingDto(long id, double avgRating, String name, String headshotUrl) {

    public static FighterRatingDto toDto(Fighter fighter, String headshotUrl){
        return FighterRatingDto.builder()
                .id(fighter.getId())
                .avgRating(fighter.getAvgRating())
                .name(fighter.getKoreanName() != null ? fighter.getKoreanName() : fighter.getName())
                .headshotUrl(headshotUrl)
                .build();
    }

}
