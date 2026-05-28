package my.mma.api.game.service;

import my.mma.api.game.dto.GameType;

import java.util.List;

public interface GameService<T> {
    GameType getType();
    List<T> generateGame(boolean isNormal);
}
