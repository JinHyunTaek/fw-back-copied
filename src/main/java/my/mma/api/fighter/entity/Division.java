package my.mma.api.fighter.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Division {

    STRAWWEIGHT("여성 스트로급"),
    FLYWEIGHT("플라이급"),
    BANTAMWEIGHT("밴텀급"),
    FEATHERWEIGHT("페더급"),
    LIGHTWEIGHT("라이트급"),
    WELTERWEIGHT("웰터급"),
    MIDDLEWEIGHT("미들급"),
    LIGHTHEAVYWEIGHT("라이트헤비급"),
    HEAVYWEIGHT("헤비급");

    private final String description;

}
