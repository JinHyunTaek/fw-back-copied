package my.mma.api.fightevent.entity.property;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum FightWeight {

    WOMENS_STRAWWEIGHT("Women's Strawweight"),
    WOMENS_FLYWEIGHT("Women's Flyweight"),
    WOMENS_BANTAMWEIGHT("Women's Bantamweight"),
    WOMENS_FEATHERWEIGHT("Women's Featherweight"),
    FLYWEIGHT("Flyweight"),
    BANTAMWEIGHT("Bantamweight"),
    FEATHERWEIGHT("Featherweight"),
    LIGHTWEIGHT("Lightweight"),
    WELTERWEIGHT("Welterweight"),
    MIDDLEWEIGHT("Middleweight"),
    LIGHT_HEAVYWEIGHT("Light Heavyweight"),
    HEAVYWEIGHT("Heavyweight"),
    SUPER_HEAVYWEIGHT("Super Heavyweight"),
    CATCH_WEIGHT("Catch Weight"),
    OPEN_WEIGHT("Open Weight");

    private final String displayName;

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static FightWeight fromDisplayName(String value) {
        if (value == null) return null;
        for (FightWeight fw : values()) {
            if (fw.displayName.equalsIgnoreCase(value.trim())) return fw;
        }
        log.warn("Unknown fightWeight value: {}", value);
        return null;
    }
}