package my.mma.admin.web.fighter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import my.mma.api.fighter.entity.Country;

import java.time.LocalDate;

@Builder
public record AdminFighterUpdateRequest(@NotNull String name, @NotNull String koreanName, String nickname,
                                        @NotNull Integer win, @NotNull Integer loss, @NotNull Integer draw,
                                        Integer reach, @NotNull @NotNull Double weight, @NotNull Integer height,
                                        @NotNull LocalDate birthday, Country nationality) {

    public static AdminFighterUpdateRequest toDto(AdminFighterResponseForUpdate fighter) {
        return AdminFighterUpdateRequest.builder()
                .name(fighter.name())
                .koreanName(fighter.koreanName())
                .nickname(fighter.nickname())
                .win(fighter.record().getWin())
                .loss(fighter.record().getLoss())
                .draw(fighter.record().getDraw())
                .reach(fighter.reach())
                .weight(fighter.weight())
                .height(fighter.height())
                .birthday(fighter.birthday())
                .nationality(fighter.nationality())
                .build();
    }

}
